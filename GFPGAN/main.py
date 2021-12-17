import cv2
import numpy as np
from gfpgan import GFPGANer
import pika
import os
import json
import base64

MODEL_PATH = "models/GFPGANCleanv1-NoCE-C2.pth"
INPUT_NAME = "FACE_RESTORE_TASK"
OUTPUT_NAME = "FACE_RESTORE_RESPONSE"

# init model
model = GFPGANer(
    model_path=MODEL_PATH,
    upscale=2,
    arch="clean",
    channel_multiplier=2)

host = os.environ.get('MQ_HOST')
host = host if host is not None else 'localhost'
connection = pika.BlockingConnection(pika.ConnectionParameters(host))
output_channel = connection.channel()
output_channel.queue_declare(queue=OUTPUT_NAME)

def restore(input_channel, method, properties, body):
    body = json.loads(body)
    
    # decode
    img = base64.b64decode(body['image'])    
    img = np.frombuffer(img, np.uint8)
    img = cv2.imdecode(img, cv2.IMREAD_COLOR)

    # model computation
    _, _, out_img = model.enhance(img, has_aligned=False, only_center_face=False, paste_back=True)
    
    # encode
    out = cv2.imencode(body['extension'], out_img)[1].tobytes()
    body['image'] = base64.b64encode(out).decode('ascii')
    
    body = json.dumps(body)
    output_channel.basic_publish(
        exchange="",
        routing_key=OUTPUT_NAME,
        body=body)
    input_channel.basic_ack(delivery_tag = method.delivery_tag) 

input_channel = connection.channel()
input_channel.queue_declare(queue=INPUT_NAME)
input_channel.basic_consume(
    queue=INPUT_NAME,
    on_message_callback=restore)
input_channel.start_consuming()
