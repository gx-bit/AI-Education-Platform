FROM python:3.12-slim AS base
WORKDIR /app
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PYTHONPATH=/app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app ./app
COPY src/main/resources/schema.sql ./schema.sql

FROM base AS test
RUN pip install --no-cache-dir pytest==8.3.5
COPY tests ./tests
CMD ["pytest", "-q"]

FROM base AS runtime
EXPOSE 8086
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8086"]
