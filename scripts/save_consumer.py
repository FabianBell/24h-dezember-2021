import pika
import json
import base64


INPUT_NAME = "FACE_RESTORE_RESPONSE"

def save(output_channel, method, properties, body):
    body = json.loads(body)
    img = base64.b64decode(body['image'])
    with open('out.jpg', 'wb') as fout:
        fout.write(img)

connection = pika.BlockingConnection(pika.ConnectionParameters('localhost'))  
input_channel = connection.channel()
input_channel.queue_declare(queue=INPUT_NAME)
input_channel.basic_consume(
    queue=INPUT_NAME,
    auto_ack=True,
    on_message_callback=save)

input_channel.start_consuming()
connection.close()
