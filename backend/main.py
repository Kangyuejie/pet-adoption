from fastapi import FastAPI, Depends, HTTPException, status, File, UploadFile, Form, Request, WebSocket, WebSocketDisconnect
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from fastapi.staticfiles import StaticFiles
from fastapi.responses import HTMLResponse, FileResponse
from sqlalchemy import create_engine, Column, Integer, String, Text, Boolean, DateTime, ForeignKey, Float, Enum as SQLEnum, or_
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker, Session, relationship
from passlib.context import CryptContext
from jose import JWTError, jwt
from datetime import datetime, timedelta
from typing import Optional, List, Dict
from pydantic import BaseModel
import enum
import os
import uuid
import shutil
import json
import asyncio

# Configuration
SECRET_KEY = "pet-adoption-secret-key-2024"
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = 60 * 24

# Database setup - MySQL
# 格式: mysql+pymysql://用户名:密码@主机:端口/数据库名
MYSQL_USER = os.getenv("MYSQL_USER", "root")
MYSQL_PASSWORD = os.getenv("MYSQL_PASSWORD", "password")
MYSQL_HOST = os.getenv("MYSQL_HOST", "localhost")
MYSQL_PORT = os.getenv("MYSQL_PORT", "3306")
MYSQL_DATABASE = os.getenv("MYSQL_DATABASE", "pet_adoption")

DATABASE_URL = f"mysql+pymysql://{MYSQL_USER}:{MYSQL_PASSWORD}@{MYSQL_HOST}:{MYSQL_PORT}/{MYSQL_DATABASE}?charset=utf8mb4"

# 如果MySQL连接失败，回退到SQLite（开发环境）
try:
    engine = create_engine(DATABASE_URL, pool_pre_ping=True, pool_recycle=3600)
    # 测试连接
    with engine.connect() as conn:
        pass
    print("Connected to MySQL database")
except Exception as e:
    print(f"MySQL connection failed: {e}")
    print("Falling back to SQLite...")
    DATABASE_URL = "sqlite:///./pet_adoption.db"
    engine = create_engine(DATABASE_URL, connect_args={"check_same_thread": False})

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

# Password hashing
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Security
security = HTTPBearer(auto_error=False)

# WebSocket Connection Manager
class ConnectionManager:
    def __init__(self):
        # user_id -> list of WebSocket connections
        self.active_connections: Dict[int, List[WebSocket]] = {}

    async def connect(self, websocket: WebSocket, user_id: int):
        await websocket.accept()
        if user_id not in self.active_connections:
            self.active_connections[user_id] = []
        self.active_connections[user_id].append(websocket)
        print(f"User {user_id} connected. Total connections: {len(self.active_connections[user_id])}")

    def disconnect(self, websocket: WebSocket, user_id: int):
        if user_id in self.active_connections:
            if websocket in self.active_connections[user_id]:
                self.active_connections[user_id].remove(websocket)
            if not self.active_connections[user_id]:
                del self.active_connections[user_id]
        print(f"User {user_id} disconnected")

    async def send_personal_message(self, message: dict, user_id: int):
        """Send message to a specific user"""
        if user_id in self.active_connections:
            for connection in self.active_connections[user_id]:
                try:
                    await connection.send_json(message)
                except Exception as e:
                    print(f"Error sending message to user {user_id}: {e}")

    async def broadcast(self, message: dict):
        """Broadcast message to all connected users"""
        for user_id, connections in self.active_connections.items():
            for connection in connections:
                try:
                    await connection.send_json(message)
                except Exception as e:
                    print(f"Error broadcasting to user {user_id}: {e}")

manager = ConnectionManager()

# Enums
class UserRole(str, enum.Enum):
    ADOPTER = "adopter"
    SHELTER = "shelter"
    ADMIN = "admin"

class PetType(str, enum.Enum):
    DOG = "dog"
    CAT = "cat"
    OTHER = "other"

class PetGender(str, enum.Enum):
    MALE = "male"
    FEMALE = "female"
    UNKNOWN = "unknown"

class ApplicationStatus(str, enum.Enum):
    PENDING = "pending"
    APPROVED = "approved"
    REJECTED = "rejected"
    COMPLETED = "completed"

# Database Models
class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True, index=True)
    email = Column(String(255), unique=True, index=True)
    username = Column(String(100), unique=True, index=True)
    hashed_password = Column(String(255))
    role = Column(String(20), default=UserRole.ADOPTER.value)
    phone = Column(String(20), nullable=True)
    address = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    is_active = Column(Boolean, default=True)

    pets = relationship("Pet", back_populates="owner")
    applications = relationship("AdoptionApplication", back_populates="applicant")
    favorites = relationship("Favorite", back_populates="user")

class Pet(Base):
    __tablename__ = "pets"
    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(100))
    pet_type = Column(String(20))
    breed = Column(String(100), nullable=True)
    age_months = Column(Integer)
    gender = Column(String(20))
    description = Column(Text, nullable=True)
    is_neutered = Column(Boolean, default=False)
    is_vaccinated = Column(Boolean, default=False)
    health_notes = Column(Text, nullable=True)
    image_url = Column(String(500), nullable=True)  # 主图片（兼容旧数据）
    location = Column(String(200), nullable=True)
    latitude = Column(Float, nullable=True)
    longitude = Column(Float, nullable=True)
    adoption_requirements = Column(Text, nullable=True)
    is_available = Column(Boolean, default=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    owner_id = Column(Integer, ForeignKey("users.id"))

    owner = relationship("User", back_populates="pets")
    applications = relationship("AdoptionApplication", back_populates="pet")
    images = relationship("PetImage", back_populates="pet", cascade="all, delete-orphan")

# 新增：宠物图片表（支持多图上传）
class PetImage(Base):
    __tablename__ = "pet_images"
    id = Column(Integer, primary_key=True, index=True)
    pet_id = Column(Integer, ForeignKey("pets.id", ondelete="CASCADE"))
    image_url = Column(String(500))
    is_primary = Column(Boolean, default=False)  # 是否为主图
    sort_order = Column(Integer, default=0)  # 排序顺序
    created_at = Column(DateTime, default=datetime.utcnow)

    pet = relationship("Pet", back_populates="images")

class AdoptionApplication(Base):
    __tablename__ = "adoption_applications"
    id = Column(Integer, primary_key=True, index=True)
    pet_id = Column(Integer, ForeignKey("pets.id"))
    applicant_id = Column(Integer, ForeignKey("users.id"))
    status = Column(String(20), default=ApplicationStatus.PENDING.value)
    home_type = Column(String(50), nullable=True)
    has_yard = Column(Boolean, default=False)
    other_pets = Column(Text, nullable=True)
    experience = Column(Text, nullable=True)
    reason = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    pet = relationship("Pet", back_populates="applications")
    applicant = relationship("User", back_populates="applications")

class Favorite(Base):
    __tablename__ = "favorites"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    pet_id = Column(Integer, ForeignKey("pets.id"))
    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User", back_populates="favorites")

class UserPreference(Base):
    __tablename__ = "user_preferences"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), unique=True)
    preferred_types = Column(String(100), nullable=True)
    min_age = Column(Integer, nullable=True)
    max_age = Column(Integer, nullable=True)
    max_distance = Column(Integer, nullable=True)
    prefer_neutered = Column(Boolean, nullable=True)

class Message(Base):
    __tablename__ = "messages"
    id = Column(Integer, primary_key=True, index=True)
    sender_id = Column(Integer, ForeignKey("users.id"))
    receiver_id = Column(Integer, ForeignKey("users.id"))
    pet_id = Column(Integer, ForeignKey("pets.id"), nullable=True)
    content = Column(Text)
    is_read = Column(Boolean, default=False)
    created_at = Column(DateTime, default=datetime.utcnow)

    sender = relationship("User", foreign_keys=[sender_id])
    receiver = relationship("User", foreign_keys=[receiver_id])

class PetComment(Base):
    __tablename__ = "pet_comments"
    id = Column(Integer, primary_key=True, index=True)
    pet_id = Column(Integer, ForeignKey("pets.id"))
    user_id = Column(Integer, ForeignKey("users.id"))
    content = Column(Text)
    created_at = Column(DateTime, default=datetime.utcnow)

class BrowsingHistory(Base):
    __tablename__ = "browsing_history"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"))
    pet_id = Column(Integer, ForeignKey("pets.id"))
    view_count = Column(Integer, default=1)
    first_viewed_at = Column(DateTime, default=datetime.utcnow)
    last_viewed_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    user = relationship("User")
    pet = relationship("Pet")

# Create tables
Base.metadata.create_all(bind=engine)

# Pydantic Schemas
class UserCreate(BaseModel):
    email: str
    username: str
    password: str
    role: Optional[str] = "adopter"
    phone: Optional[str] = None
    address: Optional[str] = None

class UserLogin(BaseModel):
    email: str
    password: str

class UserResponse(BaseModel):
    id: int
    email: str
    username: str
    role: str
    phone: Optional[str]
    address: Optional[str]

    class Config:
        from_attributes = True

class PetImageResponse(BaseModel):
    id: int
    image_url: str
    is_primary: bool
    sort_order: int

    class Config:
        from_attributes = True

class PetCreate(BaseModel):
    name: str
    pet_type: str
    breed: Optional[str] = None
    age_months: int
    gender: str
    description: Optional[str] = None
    is_neutered: bool = False
    is_vaccinated: bool = False
    health_notes: Optional[str] = None
    location: Optional[str] = None
    adoption_requirements: Optional[str] = None

class PetResponse(BaseModel):
    id: int
    name: str
    pet_type: str
    breed: Optional[str]
    age_months: int
    gender: str
    description: Optional[str]
    is_neutered: bool
    is_vaccinated: bool
    health_notes: Optional[str]
    image_url: Optional[str]
    images: List[PetImageResponse] = []
    location: Optional[str]
    adoption_requirements: Optional[str]
    is_available: bool
    created_at: datetime
    owner_id: int

    class Config:
        from_attributes = True

class ApplicationCreate(BaseModel):
    pet_id: int
    home_type: Optional[str] = None
    has_yard: bool = False
    other_pets: Optional[str] = None
    experience: Optional[str] = None
    reason: Optional[str] = None

class PreferenceUpdate(BaseModel):
    preferred_types: Optional[str] = None
    min_age: Optional[int] = None
    max_age: Optional[int] = None
    max_distance: Optional[int] = None
    prefer_neutered: Optional[bool] = None

# FastAPI app
app = FastAPI(title="Pet Adoption System", version="2.0.0")

# Mount static files
app.mount("/static", StaticFiles(directory="static"), name="static")
app.mount("/media", StaticFiles(directory="media"), name="media")

# Database dependency
def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

# Auth utilities
def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def get_password_hash(password):
    return pwd_context.hash(password)

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

def get_current_user(credentials: HTTPAuthorizationCredentials = Depends(security), db: Session = Depends(get_db)):
    if not credentials:
        return None
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
        email: str = payload.get("sub")
        if email is None:
            return None
    except JWTError:
        return None
    user = db.query(User).filter(User.email == email).first()
    return user

def require_auth(credentials: HTTPAuthorizationCredentials = Depends(security), db: Session = Depends(get_db)):
    user = get_current_user(credentials, db)
    if not user:
        raise HTTPException(status_code=401, detail="Not authenticated")
    return user

def get_user_from_token(token: str, db: Session) -> Optional[User]:
    """从token获取用户（用于WebSocket认证）"""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email: str = payload.get("sub")
        if email is None:
            return None
        user = db.query(User).filter(User.email == email).first()
        return user
    except JWTError:
        return None

# Matching algorithm
def calculate_match_score(pet: Pet, preference: UserPreference) -> int:
    score = 0
    if preference:
        if preference.preferred_types:
            types = preference.preferred_types.split(",")
            if pet.pet_type in types:
                score += 30
        if preference.min_age is not None and preference.max_age is not None:
            if preference.min_age <= pet.age_months <= preference.max_age:
                score += 25
        if preference.prefer_neutered is not None:
            if pet.is_neutered == preference.prefer_neutered:
                score += 15
    if pet.is_vaccinated:
        score += 10
    if pet.image_url:
        score += 5
    if pet.description and len(pet.description) > 50:
        score += 5
    return score

# Routes
@app.get("/", response_class=HTMLResponse)
async def home():
    return FileResponse("templates/index.html")

@app.post("/api/register")
def register(user: UserCreate, db: Session = Depends(get_db)):
    if db.query(User).filter(User.email == user.email).first():
        raise HTTPException(status_code=400, detail="Email already registered")
    if db.query(User).filter(User.username == user.username).first():
        raise HTTPException(status_code=400, detail="Username already taken")

    db_user = User(
        email=user.email,
        username=user.username,
        hashed_password=get_password_hash(user.password),
        role=user.role,
        phone=user.phone,
        address=user.address
    )
    db.add(db_user)
    db.commit()
    db.refresh(db_user)
    token = create_access_token(data={"sub": user.email})
    return {"access_token": token, "token_type": "bearer", "user": UserResponse.model_validate(db_user)}

@app.post("/api/login")
def login(user: UserLogin, db: Session = Depends(get_db)):
    db_user = db.query(User).filter(User.email == user.email).first()
    if not db_user or not verify_password(user.password, db_user.hashed_password):
        raise HTTPException(status_code=401, detail="Invalid credentials")
    token = create_access_token(data={"sub": user.email})
    return {"access_token": token, "token_type": "bearer", "user": UserResponse.model_validate(db_user)}

@app.get("/api/me")
def get_me(current_user: User = Depends(require_auth)):
    return UserResponse.model_validate(current_user)

@app.get("/api/pets")
def list_pets(
    pet_type: Optional[str] = None,
    gender: Optional[str] = None,
    min_age: Optional[int] = None,
    max_age: Optional[int] = None,
    is_neutered: Optional[bool] = None,
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: Optional[User] = Depends(get_current_user)
):
    query = db.query(Pet).filter(Pet.is_available == True)

    if pet_type:
        query = query.filter(Pet.pet_type == pet_type)
    if gender:
        query = query.filter(Pet.gender == gender)
    if min_age is not None:
        query = query.filter(Pet.age_months >= min_age)
    if max_age is not None:
        query = query.filter(Pet.age_months <= max_age)
    if is_neutered is not None:
        query = query.filter(Pet.is_neutered == is_neutered)
    if search:
        search_term = f"%{search}%"
        query = query.filter(or_(
            Pet.name.ilike(search_term),
            Pet.breed.ilike(search_term),
            Pet.pet_type.ilike(search_term),
            Pet.description.ilike(search_term),
            Pet.location.ilike(search_term)
        ))

    pets = query.order_by(Pet.created_at.desc()).all()

    # Calculate match scores if user is logged in
    result = []
    preference = None
    if current_user:
        preference = db.query(UserPreference).filter(UserPreference.user_id == current_user.id).first()

    for pet in pets:
        pet_dict = {
            "id": pet.id,
            "name": pet.name,
            "pet_type": pet.pet_type,
            "breed": pet.breed,
            "age_months": pet.age_months,
            "gender": pet.gender,
            "description": pet.description,
            "is_neutered": pet.is_neutered,
            "is_vaccinated": pet.is_vaccinated,
            "health_notes": pet.health_notes,
            "image_url": pet.image_url,
            "images": [{"id": img.id, "image_url": img.image_url, "is_primary": img.is_primary, "sort_order": img.sort_order} for img in pet.images],
            "location": pet.location,
            "adoption_requirements": pet.adoption_requirements,
            "is_available": pet.is_available,
            "created_at": pet.created_at,
            "owner_id": pet.owner_id,
            "match_score": calculate_match_score(pet, preference)
        }
        result.append(pet_dict)

    # Sort by match score
    result.sort(key=lambda x: x["match_score"], reverse=True)
    return result

@app.get("/api/pets/{pet_id}")
def get_pet(pet_id: int, db: Session = Depends(get_db)):
    pet = db.query(Pet).filter(Pet.id == pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")

    return {
        "id": pet.id,
        "name": pet.name,
        "pet_type": pet.pet_type,
        "breed": pet.breed,
        "age_months": pet.age_months,
        "gender": pet.gender,
        "description": pet.description,
        "is_neutered": pet.is_neutered,
        "is_vaccinated": pet.is_vaccinated,
        "health_notes": pet.health_notes,
        "image_url": pet.image_url,
        "images": [{"id": img.id, "image_url": img.image_url, "is_primary": img.is_primary, "sort_order": img.sort_order} for img in pet.images],
        "location": pet.location,
        "adoption_requirements": pet.adoption_requirements,
        "is_available": pet.is_available,
        "created_at": pet.created_at,
        "owner_id": pet.owner_id
    }

@app.post("/api/pets")
async def create_pet(
    name: str = Form(...),
    pet_type: str = Form(...),
    breed: str = Form(None),
    age_months: int = Form(...),
    gender: str = Form(...),
    description: str = Form(None),
    is_neutered: bool = Form(False),
    is_vaccinated: bool = Form(False),
    health_notes: str = Form(None),
    location: str = Form(None),
    adoption_requirements: str = Form(None),
    images: List[UploadFile] = File(None),  # 支持多图上传
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    if current_user.role not in [UserRole.SHELTER.value, UserRole.ADMIN.value]:
        raise HTTPException(status_code=403, detail="Only shelters can post pets")

    # 创建宠物记录
    pet = Pet(
        name=name,
        pet_type=pet_type,
        breed=breed,
        age_months=age_months,
        gender=gender,
        description=description,
        is_neutered=is_neutered,
        is_vaccinated=is_vaccinated,
        health_notes=health_notes,
        location=location,
        adoption_requirements=adoption_requirements,
        owner_id=current_user.id
    )
    db.add(pet)
    db.commit()
    db.refresh(pet)

    # 处理多图上传
    if images:
        for idx, image in enumerate(images):
            if image.filename:  # 确保文件有效
                ext = os.path.splitext(image.filename)[1]
                filename = f"{uuid.uuid4()}{ext}"
                filepath = f"media/pets/{filename}"

                # 确保目录存在
                os.makedirs("media/pets", exist_ok=True)

                with open(filepath, "wb") as f:
                    shutil.copyfileobj(image.file, f)

                image_url = f"/media/pets/{filename}"

                # 创建图片记录
                pet_image = PetImage(
                    pet_id=pet.id,
                    image_url=image_url,
                    is_primary=(idx == 0),  # 第一张为主图
                    sort_order=idx
                )
                db.add(pet_image)

                # 设置主图URL到pet记录（兼容旧版本）
                if idx == 0:
                    pet.image_url = image_url

        db.commit()
        db.refresh(pet)

    # 广播新宠物消息
    await manager.broadcast({
        "type": "new_pet",
        "data": {
            "id": pet.id,
            "name": pet.name,
            "pet_type": pet.pet_type,
            "image_url": pet.image_url
        }
    })

    return {
        "id": pet.id,
        "name": pet.name,
        "pet_type": pet.pet_type,
        "breed": pet.breed,
        "age_months": pet.age_months,
        "gender": pet.gender,
        "description": pet.description,
        "is_neutered": pet.is_neutered,
        "is_vaccinated": pet.is_vaccinated,
        "health_notes": pet.health_notes,
        "image_url": pet.image_url,
        "images": [{"id": img.id, "image_url": img.image_url, "is_primary": img.is_primary, "sort_order": img.sort_order} for img in pet.images],
        "location": pet.location,
        "adoption_requirements": pet.adoption_requirements,
        "is_available": pet.is_available,
        "created_at": pet.created_at,
        "owner_id": pet.owner_id
    }

# 新增：上传宠物图片API
@app.post("/api/pets/{pet_id}/images")
async def upload_pet_images(
    pet_id: int,
    images: List[UploadFile] = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    pet = db.query(Pet).filter(Pet.id == pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")
    if pet.owner_id != current_user.id and current_user.role != UserRole.ADMIN.value:
        raise HTTPException(status_code=403, detail="Not authorized")

    # 获取当前图片数量以确定排序
    existing_count = db.query(PetImage).filter(PetImage.pet_id == pet_id).count()

    uploaded_images = []
    for idx, image in enumerate(images):
        if image.filename:
            ext = os.path.splitext(image.filename)[1]
            filename = f"{uuid.uuid4()}{ext}"
            filepath = f"media/pets/{filename}"

            os.makedirs("media/pets", exist_ok=True)

            with open(filepath, "wb") as f:
                shutil.copyfileobj(image.file, f)

            image_url = f"/media/pets/{filename}"

            pet_image = PetImage(
                pet_id=pet_id,
                image_url=image_url,
                is_primary=(existing_count == 0 and idx == 0),
                sort_order=existing_count + idx
            )
            db.add(pet_image)
            uploaded_images.append(pet_image)

            # 如果是第一张图片且宠物没有主图，设置为主图
            if existing_count == 0 and idx == 0:
                pet.image_url = image_url

    db.commit()

    return {"message": f"Uploaded {len(uploaded_images)} images", "images": [
        {"id": img.id, "image_url": img.image_url, "is_primary": img.is_primary}
        for img in uploaded_images
    ]}

# 新增：删除宠物图片API
@app.delete("/api/pets/{pet_id}/images/{image_id}")
async def delete_pet_image(
    pet_id: int,
    image_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    pet = db.query(Pet).filter(Pet.id == pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")
    if pet.owner_id != current_user.id and current_user.role != UserRole.ADMIN.value:
        raise HTTPException(status_code=403, detail="Not authorized")

    image = db.query(PetImage).filter(PetImage.id == image_id, PetImage.pet_id == pet_id).first()
    if not image:
        raise HTTPException(status_code=404, detail="Image not found")

    # 删除文件
    try:
        file_path = image.image_url.lstrip("/")
        if os.path.exists(file_path):
            os.remove(file_path)
    except Exception as e:
        print(f"Error deleting file: {e}")

    # 如果删除的是主图，设置下一张为主图
    was_primary = image.is_primary
    db.delete(image)
    db.commit()

    if was_primary:
        next_image = db.query(PetImage).filter(PetImage.pet_id == pet_id).order_by(PetImage.sort_order).first()
        if next_image:
            next_image.is_primary = True
            pet.image_url = next_image.image_url
        else:
            pet.image_url = None
        db.commit()

    return {"message": "Image deleted"}

# 新增：设置主图API
@app.put("/api/pets/{pet_id}/images/{image_id}/primary")
async def set_primary_image(
    pet_id: int,
    image_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_auth)
):
    pet = db.query(Pet).filter(Pet.id == pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")
    if pet.owner_id != current_user.id and current_user.role != UserRole.ADMIN.value:
        raise HTTPException(status_code=403, detail="Not authorized")

    # 取消所有图片的主图状态
    db.query(PetImage).filter(PetImage.pet_id == pet_id).update({"is_primary": False})

    # 设置新的主图
    image = db.query(PetImage).filter(PetImage.id == image_id, PetImage.pet_id == pet_id).first()
    if not image:
        raise HTTPException(status_code=404, detail="Image not found")

    image.is_primary = True
    pet.image_url = image.image_url
    db.commit()

    return {"message": "Primary image updated"}

@app.put("/api/pets/{pet_id}")
def update_pet(pet_id: int, pet_data: PetCreate, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    pet = db.query(Pet).filter(Pet.id == pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")
    if pet.owner_id != current_user.id and current_user.role != UserRole.ADMIN.value:
        raise HTTPException(status_code=403, detail="Not authorized")

    for key, value in pet_data.model_dump(exclude_unset=True).items():
        setattr(pet, key, value)
    db.commit()
    db.refresh(pet)
    return PetResponse.model_validate(pet)

@app.delete("/api/pets/{pet_id}")
def delete_pet(pet_id: int, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    pet = db.query(Pet).filter(Pet.id == pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")
    if pet.owner_id != current_user.id and current_user.role != UserRole.ADMIN.value:
        raise HTTPException(status_code=403, detail="Not authorized")

    # 删除关联的图片文件
    for image in pet.images:
        try:
            file_path = image.image_url.lstrip("/")
            if os.path.exists(file_path):
                os.remove(file_path)
        except Exception as e:
            print(f"Error deleting file: {e}")

    db.delete(pet)
    db.commit()
    return {"message": "Pet deleted"}

@app.post("/api/applications")
async def create_application(app_data: ApplicationCreate, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    pet = db.query(Pet).filter(Pet.id == app_data.pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")
    if not pet.is_available:
        raise HTTPException(status_code=400, detail="Pet is not available for adoption")

    existing = db.query(AdoptionApplication).filter(
        AdoptionApplication.pet_id == app_data.pet_id,
        AdoptionApplication.applicant_id == current_user.id,
        AdoptionApplication.status == ApplicationStatus.PENDING.value
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="You already have a pending application for this pet")

    application = AdoptionApplication(
        pet_id=app_data.pet_id,
        applicant_id=current_user.id,
        home_type=app_data.home_type,
        has_yard=app_data.has_yard,
        other_pets=app_data.other_pets,
        experience=app_data.experience,
        reason=app_data.reason
    )
    db.add(application)
    db.commit()
    db.refresh(application)

    # 通过WebSocket通知宠物主人有新申请
    await manager.send_personal_message({
        "type": "new_application",
        "data": {
            "application_id": application.id,
            "pet_id": pet.id,
            "pet_name": pet.name,
            "applicant_name": current_user.username
        }
    }, pet.owner_id)

    return {"message": "Application submitted", "id": application.id}

@app.get("/api/applications")
def list_applications(db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    if current_user.role == UserRole.ADOPTER.value:
        apps = db.query(AdoptionApplication).filter(AdoptionApplication.applicant_id == current_user.id).all()
    else:
        pets = db.query(Pet).filter(Pet.owner_id == current_user.id).all()
        pet_ids = [p.id for p in pets]
        apps = db.query(AdoptionApplication).filter(AdoptionApplication.pet_id.in_(pet_ids)).all()

    result = []
    for app in apps:
        result.append({
            "id": app.id,
            "pet_id": app.pet_id,
            "pet_name": app.pet.name if app.pet else None,
            "applicant_id": app.applicant_id,
            "applicant_name": app.applicant.username if app.applicant else None,
            "status": app.status,
            "home_type": app.home_type,
            "has_yard": app.has_yard,
            "other_pets": app.other_pets,
            "experience": app.experience,
            "reason": app.reason,
            "created_at": app.created_at.isoformat()
        })
    return result

@app.put("/api/applications/{app_id}/status")
async def update_application_status(app_id: int, status: str, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    application = db.query(AdoptionApplication).filter(AdoptionApplication.id == app_id).first()
    if not application:
        raise HTTPException(status_code=404, detail="Application not found")

    pet = db.query(Pet).filter(Pet.id == application.pet_id).first()
    if pet.owner_id != current_user.id and current_user.role != UserRole.ADMIN.value:
        raise HTTPException(status_code=403, detail="Not authorized")

    application.status = status
    if status == ApplicationStatus.COMPLETED.value:
        pet.is_available = False
    db.commit()

    # 通过WebSocket通知申请者状态更新
    await manager.send_personal_message({
        "type": "application_status_update",
        "data": {
            "application_id": application.id,
            "pet_id": pet.id,
            "pet_name": pet.name,
            "status": status
        }
    }, application.applicant_id)

    return {"message": "Status updated"}

@app.post("/api/favorites/{pet_id}")
def add_favorite(pet_id: int, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    existing = db.query(Favorite).filter(
        Favorite.user_id == current_user.id,
        Favorite.pet_id == pet_id
    ).first()
    if existing:
        db.delete(existing)
        db.commit()
        return {"message": "Removed from favorites", "is_favorite": False}

    fav = Favorite(user_id=current_user.id, pet_id=pet_id)
    db.add(fav)
    db.commit()
    return {"message": "Added to favorites", "is_favorite": True}

@app.get("/api/favorites")
def list_favorites(db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    favs = db.query(Favorite).filter(Favorite.user_id == current_user.id).all()
    pet_ids = [f.pet_id for f in favs]
    pets = db.query(Pet).filter(Pet.id.in_(pet_ids)).all()
    return [PetResponse.model_validate(p) for p in pets]

@app.put("/api/preferences")
def update_preferences(prefs: PreferenceUpdate, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    preference = db.query(UserPreference).filter(UserPreference.user_id == current_user.id).first()
    if not preference:
        preference = UserPreference(user_id=current_user.id)
        db.add(preference)

    for key, value in prefs.model_dump(exclude_unset=True).items():
        setattr(preference, key, value)
    db.commit()
    return {"message": "Preferences updated"}

@app.get("/api/preferences")
def get_preferences(db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    pref = db.query(UserPreference).filter(UserPreference.user_id == current_user.id).first()
    if pref:
        return {
            "preferred_types": pref.preferred_types,
            "min_age": pref.min_age,
            "max_age": pref.max_age,
            "max_distance": pref.max_distance,
            "prefer_neutered": pref.prefer_neutered
        }
    return {}

@app.get("/api/stats")
def get_stats(db: Session = Depends(get_db)):
    total_pets = db.query(Pet).count()
    available_pets = db.query(Pet).filter(Pet.is_available == True).count()
    total_applications = db.query(AdoptionApplication).count()
    completed_adoptions = db.query(AdoptionApplication).filter(
        AdoptionApplication.status == ApplicationStatus.COMPLETED.value
    ).count()

    dogs = db.query(Pet).filter(Pet.pet_type == "dog").count()
    cats = db.query(Pet).filter(Pet.pet_type == "cat").count()

    return {
        "total_pets": total_pets,
        "available_pets": available_pets,
        "total_applications": total_applications,
        "completed_adoptions": completed_adoptions,
        "pets_by_type": {"dogs": dogs, "cats": cats, "other": total_pets - dogs - cats}
    }

@app.get("/api/my-pets")
def get_my_pets(db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    pets = db.query(Pet).filter(Pet.owner_id == current_user.id).all()
    return [PetResponse.model_validate(p) for p in pets]

# Message APIs
@app.post("/api/messages")
async def send_message(receiver_id: int, content: str, pet_id: Optional[int] = None,
                 db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    message = Message(
        sender_id=current_user.id,
        receiver_id=receiver_id,
        pet_id=pet_id,
        content=content
    )
    db.add(message)
    db.commit()
    db.refresh(message)

    # 通过WebSocket实时推送消息给接收者
    await manager.send_personal_message({
        "type": "new_message",
        "data": {
            "id": message.id,
            "sender_id": current_user.id,
            "sender_name": current_user.username,
            "content": content,
            "pet_id": pet_id,
            "created_at": message.created_at.isoformat()
        }
    }, receiver_id)

    return {"message": "Message sent", "id": message.id}

@app.get("/api/messages")
def get_messages(db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    # Get all conversations
    sent = db.query(Message).filter(Message.sender_id == current_user.id).all()
    received = db.query(Message).filter(Message.receiver_id == current_user.id).all()

    # Group by conversation partner
    conversations = {}
    for msg in sent + received:
        partner_id = msg.receiver_id if msg.sender_id == current_user.id else msg.sender_id
        if partner_id not in conversations:
            partner = db.query(User).filter(User.id == partner_id).first()
            conversations[partner_id] = {
                "partner_id": partner_id,
                "partner_name": partner.username if partner else "Unknown",
                "messages": [],
                "unread_count": 0
            }
        conversations[partner_id]["messages"].append({
            "id": msg.id,
            "sender_id": msg.sender_id,
            "receiver_id": msg.receiver_id,
            "content": msg.content,
            "is_read": msg.is_read,
            "created_at": msg.created_at.isoformat(),
            "is_mine": msg.sender_id == current_user.id
        })
        if not msg.is_read and msg.receiver_id == current_user.id:
            conversations[partner_id]["unread_count"] += 1

    # Sort messages by time
    for conv in conversations.values():
        conv["messages"].sort(key=lambda x: x["created_at"])

    return list(conversations.values())

@app.get("/api/messages/{partner_id}")
def get_conversation(partner_id: int, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    messages = db.query(Message).filter(
        ((Message.sender_id == current_user.id) & (Message.receiver_id == partner_id)) |
        ((Message.sender_id == partner_id) & (Message.receiver_id == current_user.id))
    ).order_by(Message.created_at).all()

    # Mark as read
    for msg in messages:
        if msg.receiver_id == current_user.id and not msg.is_read:
            msg.is_read = True
    db.commit()

    partner = db.query(User).filter(User.id == partner_id).first()
    return {
        "partner_id": partner_id,
        "partner_name": partner.username if partner else "Unknown",
        "partner_role": partner.role if partner else None,
        "messages": [{
            "id": m.id,
            "sender_id": m.sender_id,
            "content": m.content,
            "created_at": m.created_at.isoformat(),
            "is_mine": m.sender_id == current_user.id
        } for m in messages]
    }

@app.get("/api/messages/unread/count")
def get_unread_count(db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    count = db.query(Message).filter(
        Message.receiver_id == current_user.id,
        Message.is_read == False
    ).count()
    return {"unread_count": count}

# Pet Comment APIs
@app.post("/api/pets/{pet_id}/comments")
def add_comment(pet_id: int, content: str, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    pet = db.query(Pet).filter(Pet.id == pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")

    comment = PetComment(pet_id=pet_id, user_id=current_user.id, content=content)
    db.add(comment)
    db.commit()
    db.refresh(comment)
    return {"message": "Comment added", "id": comment.id}

@app.get("/api/pets/{pet_id}/comments")
def get_comments(pet_id: int, db: Session = Depends(get_db)):
    comments = db.query(PetComment).filter(PetComment.pet_id == pet_id).order_by(PetComment.created_at.desc()).all()
    result = []
    for c in comments:
        user = db.query(User).filter(User.id == c.user_id).first()
        result.append({
            "id": c.id,
            "user_id": c.user_id,
            "username": user.username if user else "Unknown",
            "content": c.content,
            "created_at": c.created_at.isoformat()
        })
    return result

@app.delete("/api/comments/{comment_id}")
def delete_comment(comment_id: int, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    comment = db.query(PetComment).filter(PetComment.id == comment_id).first()
    if not comment:
        raise HTTPException(status_code=404, detail="Comment not found")
    if comment.user_id != current_user.id and current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Not authorized")
    db.delete(comment)
    db.commit()
    return {"message": "Comment deleted"}

# User Profile API
@app.get("/api/users/{user_id}")
def get_user_profile(user_id: int, db: Session = Depends(get_db)):
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    pets = db.query(Pet).filter(Pet.owner_id == user_id, Pet.is_available == True).all()
    completed_adoptions = db.query(AdoptionApplication).filter(
        AdoptionApplication.applicant_id == user_id,
        AdoptionApplication.status == "completed"
    ).count()

    return {
        "id": user.id,
        "username": user.username,
        "role": user.role,
        "created_at": user.created_at.isoformat(),
        "pets_count": len(pets),
        "completed_adoptions": completed_adoptions,
        "pets": [PetResponse.model_validate(p) for p in pets[:6]]
    }

@app.put("/api/profile")
def update_profile(phone: Optional[str] = None, address: Optional[str] = None,
                   db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    if phone is not None:
        current_user.phone = phone
    if address is not None:
        current_user.address = address
    db.commit()
    return {"message": "Profile updated"}

# Browsing History APIs
@app.post("/api/history/{pet_id}")
def record_view(pet_id: int, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    """Record a pet view in browsing history"""
    pet = db.query(Pet).filter(Pet.id == pet_id).first()
    if not pet:
        raise HTTPException(status_code=404, detail="Pet not found")

    # Check if already in history
    existing = db.query(BrowsingHistory).filter(
        BrowsingHistory.user_id == current_user.id,
        BrowsingHistory.pet_id == pet_id
    ).first()

    if existing:
        existing.view_count += 1
        existing.last_viewed_at = datetime.utcnow()
    else:
        history = BrowsingHistory(
            user_id=current_user.id,
            pet_id=pet_id
        )
        db.add(history)

    db.commit()
    return {"message": "View recorded"}

@app.get("/api/history")
def get_history(limit: int = 20, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    """Get user's browsing history"""
    history = db.query(BrowsingHistory).filter(
        BrowsingHistory.user_id == current_user.id
    ).order_by(BrowsingHistory.last_viewed_at.desc()).limit(limit).all()

    result = []
    for h in history:
        pet = db.query(Pet).filter(Pet.id == h.pet_id).first()
        if pet:
            pet_dict = PetResponse.model_validate(pet).model_dump()
            pet_dict["view_count"] = h.view_count
            pet_dict["last_viewed_at"] = h.last_viewed_at.isoformat()
            pet_dict["first_viewed_at"] = h.first_viewed_at.isoformat()
            result.append(pet_dict)

    return result

@app.delete("/api/history")
def clear_history(db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    """Clear all browsing history"""
    db.query(BrowsingHistory).filter(BrowsingHistory.user_id == current_user.id).delete()
    db.commit()
    return {"message": "History cleared"}

@app.delete("/api/history/{pet_id}")
def remove_from_history(pet_id: int, db: Session = Depends(get_db), current_user: User = Depends(require_auth)):
    """Remove a specific pet from browsing history"""
    db.query(BrowsingHistory).filter(
        BrowsingHistory.user_id == current_user.id,
        BrowsingHistory.pet_id == pet_id
    ).delete()
    db.commit()
    return {"message": "Removed from history"}

# WebSocket endpoint for real-time notifications
@app.websocket("/ws/{token}")
async def websocket_endpoint(websocket: WebSocket, token: str):
    db = SessionLocal()
    try:
        # 验证token获取用户
        user = get_user_from_token(token, db)
        if not user:
            await websocket.close(code=4001, reason="Invalid token")
            return

        await manager.connect(websocket, user.id)

        try:
            while True:
                # 接收客户端消息（心跳或其他）
                data = await websocket.receive_text()

                # 处理心跳
                if data == "ping":
                    await websocket.send_text("pong")
                else:
                    # 可以处理其他类型的消息
                    try:
                        message_data = json.loads(data)
                        # 处理客户端发送的消息
                        if message_data.get("type") == "typing":
                            # 通知对方正在输入
                            receiver_id = message_data.get("receiver_id")
                            if receiver_id:
                                await manager.send_personal_message({
                                    "type": "typing",
                                    "data": {
                                        "sender_id": user.id,
                                        "sender_name": user.username
                                    }
                                }, receiver_id)
                    except json.JSONDecodeError:
                        pass

        except WebSocketDisconnect:
            manager.disconnect(websocket, user.id)
    finally:
        db.close()

# Initialize sample data
def init_sample_data():
    db = SessionLocal()
    try:
        if db.query(User).count() == 0:
            # Create admin user
            admin = User(
                email="admin@petadopt.com",
                username="admin",
                hashed_password=get_password_hash("admin123"),
                role=UserRole.ADMIN.value
            )
            db.add(admin)

            # Create shelter user
            shelter = User(
                email="shelter@petadopt.com",
                username="shelter",
                hashed_password=get_password_hash("shelter123"),
                role=UserRole.SHELTER.value,
                address="123 Main Street"
            )
            db.add(shelter)
            db.commit()
            db.refresh(shelter)

            # Add sample pets
            sample_pets = [
                Pet(name="旺财", pet_type="dog", breed="金毛寻回犬", age_months=24, gender="male",
                    description="性格温顺友善的金毛，喜欢和人玩耍，非常适合家庭饲养。已完成基础训练，会握手、坐下等指令。",
                    is_neutered=True, is_vaccinated=True, location="北京市朝阳区", owner_id=shelter.id,
                    adoption_requirements="需要有足够的活动空间，每天至少遛狗1小时"),
                Pet(name="咪咪", pet_type="cat", breed="英国短毛猫", age_months=12, gender="female",
                    description="温柔安静的英短蓝猫，喜欢被抚摸和拥抱。不挑食，已适应猫砂盆。",
                    is_neutered=True, is_vaccinated=True, location="上海市浦东新区", owner_id=shelter.id,
                    adoption_requirements="适合公寓饲养，需要定期梳毛"),
                Pet(name="大黄", pet_type="dog", breed="中华田园犬", age_months=18, gender="male",
                    description="忠诚可靠的田园犬，从小被救助。身体健康，适应能力强，看家护院的好帮手。",
                    is_neutered=False, is_vaccinated=True, location="广州市天河区", owner_id=shelter.id,
                    adoption_requirements="最好有院子或阳台，需要耐心陪伴"),
                Pet(name="橘子", pet_type="cat", breed="橘猫", age_months=6, gender="male",
                    description="活泼好动的小橘猫，好奇心强，喜欢探索。食量较大，是个小吃货。",
                    is_neutered=False, is_vaccinated=True, location="深圳市南山区", owner_id=shelter.id,
                    adoption_requirements="需要准备猫爬架，注意控制饮食"),
                Pet(name="豆豆", pet_type="dog", breed="泰迪犬", age_months=36, gender="male",
                    description="聪明伶俐的泰迪，已完成全部训练。不掉毛，适合对毛发过敏的家庭。性格亲人，喜欢撒娇。",
                    is_neutered=True, is_vaccinated=True, location="杭州市西湖区", owner_id=shelter.id,
                    adoption_requirements="需要定期美容修剪，每天陪伴时间不少于2小时"),
                Pet(name="花花", pet_type="cat", breed="三花猫", age_months=8, gender="female",
                    description="漂亮的三花猫，性格独立但也亲人。白天喜欢晒太阳，晚上爱玩逗猫棒。",
                    is_neutered=True, is_vaccinated=True, location="成都市锦江区", owner_id=shelter.id,
                    adoption_requirements="需要封窗，准备基本的猫用品"),
                Pet(name="小黑", pet_type="dog", breed="拉布拉多", age_months=10, gender="male",
                    description="精力充沛的拉布拉多幼犬，非常聪明好学。喜欢玩球和游泳，是运动爱好者的最佳伴侣。",
                    is_neutered=False, is_vaccinated=True, location="南京市鼓楼区", owner_id=shelter.id,
                    adoption_requirements="需要大量运动，适合有运动习惯的主人"),
                Pet(name="团团", pet_type="cat", breed="布偶猫", age_months=15, gender="female",
                    description="温顺黏人的布偶猫，毛发柔软如丝。喜欢被抱着，是名副其实的布偶猫。",
                    is_neutered=True, is_vaccinated=True, location="武汉市武昌区", owner_id=shelter.id,
                    adoption_requirements="需要每天梳毛，注意毛球问题"),
                Pet(name="阿福", pet_type="dog", breed="柯基犬", age_months=20, gender="male",
                    description="短腿小可爱柯基，屁股圆圆的特别萌。性格开朗，喜欢和其他狗狗玩耍。",
                    is_neutered=True, is_vaccinated=True, location="西安市雁塔区", owner_id=shelter.id,
                    adoption_requirements="注意控制体重，避免爬楼梯"),
                Pet(name="小白", pet_type="other", breed="垂耳兔", age_months=8, gender="female",
                    description="可爱的垂耳兔，毛茸茸的大耳朵。性格温顺，喜欢吃胡萝卜和干草。",
                    is_neutered=False, is_vaccinated=True, location="重庆市渝中区", owner_id=shelter.id,
                    adoption_requirements="需要准备兔笼和磨牙棒，定期清理"),
            ]
            for pet in sample_pets:
                db.add(pet)
            db.commit()
            print("Sample data initialized")
    finally:
        db.close()

init_sample_data()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=12346)
