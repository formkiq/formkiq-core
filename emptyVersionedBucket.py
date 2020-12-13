import boto3
import argparse
import sys
from botocore.exceptions import BotoCoreError
from botocore.exceptions import ClientError


def run():
    global args
    args = parse_args()

    if args.profile:
        session = boto3.Session(profile_name=args.profile)
    else:
        session = boto3.Session()

    s3 = session.resource(service_name='s3')
    bucket = s3.Bucket(args.bucket)
    bucket.object_versions.delete()

    if args.delete_bucket:
        bucket.delete()


def parse_args():
    description = "Delete all objects and versions from Version Enabled S3 Bucket"

    parser = argparse.ArgumentParser(description=description)
    parser.add_argument('-b', '--bucket', required=True,
                        help='A vaild s3 bucket name')
    parser.add_argument('-p', '--profile',
                        help='A AWS profile name located in ~/.aws/config')
    parser.add_argument('-d', '--delete_bucket', action='store_true',
                        help='Remove the bucket after emptying')

    return parser.parse_args()


code = 0

try:
    run()
except (BotoCoreError, ClientError) as e:
    print(e.response['Error']['Code'])
    code = 1
except Exception as e:
    raise
    code = 2


sys.exit(code)

