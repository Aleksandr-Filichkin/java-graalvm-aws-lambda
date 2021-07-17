import json
import logging
import uuid

import boto3

dynamodb = boto3.resource('dynamodb')
table = dynamodb.Table('books')

def create(event, context):
    data = json.loads(event['body'])
    logging.info(data)

    item = {
        'id': str(uuid.uuid1()),
        'author': data['author'],
        'name': data['name'],
    }

    # write the db
    table.put_item(Item=item)

    # create a response
    response = {
        "statusCode": 201,
        "body": json.dumps(item)
    }

    return response