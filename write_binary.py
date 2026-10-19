import sys
import base64

def write_file(path, b64_data):
    with open(path, 'wb') as f:
        f.write(base64.b64decode(b64_data))

if __name__ == '__main__':
    write_file(sys.argv[1], sys.argv[2])
