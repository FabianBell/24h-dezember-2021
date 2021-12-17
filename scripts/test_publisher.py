import pika
import json
import base64

with open('test.jpg', 'rb') as fin:
    data = fin.read()
    
body = {
    'image' : base64.b64encode(data).decode('ascii'),
    'extension' : '.jpg',
    'sessionkey' : 1
}

OUPUT_NAME = "FACE_RESTORE_TASK" 

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))  
output_channel = connection.channel()
output_channel.queue_declare(queue=OUPUT_NAME)
output_channel.basic_publish(
    exchange="",
    routing_key=OUPUT_NAME,
    body=json.dumps(body))

connection.close()
