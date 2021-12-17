FROM python:3.8
WORKDIR /app
COPY ./ /app
RUN apt-get update && apt-get install -y python3-opencv
RUN pip install torch
RUN pip install -r requirements.txt
RUN pip install git+https://github.com/TencentARC/GFPGAN.git
RUN wget https://github.com/TencentARC/GFPGAN/releases/download/v0.2.0/GFPGANCleanv1-NoCE-C2.pth -P models
CMD ["python", "main.py"]
