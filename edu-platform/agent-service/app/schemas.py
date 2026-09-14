from pydantic import BaseModel, ConfigDict, Field


class PlanRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    goal: str = Field(min_length=1, max_length=500)
    interest: str = Field(min_length=1, max_length=300)
    current_level: str = Field(default="beginner", alias="currentLevel", pattern="^(beginner|intermediate|advanced)$")
    weeks: int = Field(default=4, ge=1, le=12)
    hours_per_week: int = Field(default=7, alias="hoursPerWeek", ge=1, le=40)


class ChatRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    conversation_id: str | None = Field(default=None, alias="conversationId")
    message: str = Field(min_length=1, max_length=2000)
