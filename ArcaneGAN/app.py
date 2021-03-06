from facenet_pytorch import MTCNN
from torchvision import transforms
import torch, PIL
import io
import json
import os
import pika
import base64

mtcnn = MTCNN(image_size=256, margin=80)

# simplest ye olde trustworthy MTCNN for face detection with landmarks
def detect(img):
 
        # Detect faces
        batch_boxes, batch_probs, batch_points = mtcnn.detect(img, landmarks=True)
        # Select faces
        if not mtcnn.keep_all:
            batch_boxes, batch_probs, batch_points = mtcnn.select_boxes(
                batch_boxes, batch_probs, batch_points, img, method=mtcnn.selection_method
            )
 
        return batch_boxes, batch_points

# my version of isOdd, should make a separate repo for it :D
def makeEven(_x):
  return _x if (_x % 2 == 0) else _x+1

# the actual scaler function
def scale(boxes, _img, max_res=1_500_000, target_face=256, fixed_ratio=0, max_upscale=2, VERBOSE=False):
 
    x, y = _img.size
 
    ratio = 2 #initial ratio
 
    #scale to desired face size
    if (boxes is not None):
      if len(boxes)>0:
        ratio = target_face/max(boxes[0][2:]-boxes[0][:2]); 
        ratio = min(ratio, max_upscale)

    if fixed_ratio>0:
      ratio = fixed_ratio
 
    x*=ratio
    y*=ratio
 
    #downscale to fit into max res 
    res = x*y
    if res > max_res:
      ratio = pow(res/max_res,1/2); 
      x=int(x/ratio)
      y=int(y/ratio)
 
    #make dimensions even, because usually NNs fail on uneven dimensions due skip connection size mismatch
    x = makeEven(int(x))
    y = makeEven(int(y))
    
    size = (x, y)

    return _img.resize(size)

""" 
    A useful scaler algorithm, based on face detection.
    Takes PIL.Image, returns a uniformly scaled PIL.Image
    boxes: a list of detected bboxes
    _img: PIL.Image
    max_res: maximum pixel area to fit into. Use to stay below the VRAM limits of your GPU.
    target_face: desired face size. Upscale or downscale the whole image to fit the detected face into that dimension.
    fixed_ratio: fixed scale. Ignores the face size, but doesn't ignore the max_res limit.
    max_upscale: maximum upscale ratio. Prevents from scaling images with tiny faces to a blurry mess.
"""

def scale_by_face_size(_img, max_res=1_500_000, target_face=256, fix_ratio=0, max_upscale=2, VERBOSE=False):
    boxes = None
    boxes, _ = detect(_img)
    img_resized = scale(boxes, _img, max_res, target_face, fix_ratio, max_upscale, VERBOSE)
    return img_resized


size = 256

means = [0.485, 0.456, 0.406]
stds = [0.229, 0.224, 0.225]

t_stds = torch.tensor(stds).cpu()[:,None,None]
t_means = torch.tensor(means).cpu()[:,None,None]

def makeEven(_x):
  return int(_x) if (_x % 2 == 0) else int(_x+1)

img_transforms = transforms.Compose([                        
            transforms.ToTensor(),
            transforms.Normalize(means,stds)])
 
def tensor2im(var):
     return var.mul(t_stds).add(t_means).mul(255.).clamp(0,255).permute(1,2,0)

def proc_pil_img(input_image, model):
    transformed_image = img_transforms(input_image)[None,...].cpu()
            
    with torch.no_grad():
        result_image = model(transformed_image)[0]
        output_image = tensor2im(result_image)
        output_image = output_image.detach().cpu().numpy().astype('uint8')
        output_image = PIL.Image.fromarray(output_image)
    return output_image

def fit(img,maxsize=512):
  maxdim = max(*img.size)
  if maxdim>maxsize:
    ratio = maxsize/maxdim
    x,y = img.size
    size = (int(x*ratio),int(y*ratio)) 
    img = img.resize(size)
  return img
 
model = torch.jit.load('./ArcaneGANv0.3.jit',map_location='cpu').to('cpu').float().eval().cpu()

def process(im):
    im = scale_by_face_size(im, target_face=300, max_res=1_500_000, max_upscale=2)
    res = proc_pil_img(im, model)
    return res 

INPUT_NAME = "ARCANE_TASK"  
OUTPUT_NAME = "ARCANE_RESPONSE"

host = os.environ.get('MQ_HOST')
host = host if host is not None else 'localhost'
print(f"Using hostname {host}")
connection = pika.BlockingConnection(pika.ConnectionParameters(host))
output_channel = connection.channel()
output_channel.queue_declare(queue=OUTPUT_NAME)

def consume(input_channel, method, properties, body):
    body = json.loads(body)

    # decode
    img = base64.b64decode(body['image'])
    img = PIL.Image.open(io.BytesIO(img))
    
    # model computation
    img = process(img)

    # encode
    extension = body['extension']
    if extension.lower() == 'jpg':
        extension = 'JPEG'
    out = io.BytesIO()
    img.save(out, format=extension)
    out = out.getvalue()
    out = base64.b64encode(out).decode('ascii')
    body['image'] = out

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
    on_message_callback=consume)
input_channel.start_consuming()
