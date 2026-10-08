FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 5001

ENV TICK_INTERVAL_SECONDS=2
ENV PORT=5001
ENV ALLOWED_ORIGINS=http://localhost:3000

CMD ["python", "app.py"]
