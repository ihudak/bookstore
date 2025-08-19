"""
    Simple AWS Lambda with a sample web request function instrumented with OneAgent
    Called from the Dynapay BankinfoController - returns 200 or 400 based on the bank name is in a predefined list or not.
    Gets the payload through an API Gateway.
    Sample payload with success: {"used_bank":"Value Banks Inc."}
    Sample payload which would result in error: {"used_bank":"Not Exist Inc."}
    RUNTIME: Python 3.11
    LAYER INFORMATION:
        (1) arn:aws:lambda:us-east-1:725887861453:layer:Dynatrace_OneAgent_1_283_0_20231201-074341_with_collector_python:1
            - See documentation https://docs.dynatrace.com/docs/shortlink/aws-lambda-extension
    TRIGGER:
        API Gateway: HTTP API with Open security

    steps.
    1. create a new function and copy-paste its body and click "Deploy"
    2. set env vars in the config (Get Data to Dynatrace -> Lambda -> ...
    3. add an arn layer (AWS Lambda UI -> Layers
    4. add trigger -> add API Gateway: HTTP API with Open security
"""


import json
import logging
import base64


# Configure the logger
logger = logging.getLogger()
logger.setLevel(logging.INFO)  # Set the desired log level (DEBUG, INFO, WARNING, ERROR, CRITICAL)

logger.info('Loading function')



def return_response(json_data, status_code):
    return {
        'statusCode': status_code,
        'body': json_data,
        'headers' : {
            'Access-Control-Allow-Headers': 'Content-Type,x-dtc',
            'Access-Control-Allow-Credentials': 'true',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'OPTIONS,POST,PUT,GET'
        }
    }


def get_bank_info(event, context):
    
    
    allowed_bank_names = [
        "Ward Holdings",
        "Connection Financial Group",
        "Eminence Trust",
        "Goldleaf Banks",
        "His Majesty Holdings",
        "Value Banks Inc.",
        "Concorde Financial Group",
        "Obsidian Trust Corp.",
        "Pursuit Bank Inc."
    ]
    if event.get("isBase64Encoded"):
        logger.warning("got base64 encoded body")
        b64encoded_event_body=event.get("body")
        event_body_bytes = base64.b64decode(b64encoded_event_body)
        event_body_string =  event_body_bytes.decode('utf-8')
        event_body_obj = json.loads(event_body_string)
    else:
        event_body_string =  event.get("body")
        event_body_obj = json.loads(event_body_string)
        
    bank_name = event_body_obj.get("used_bank")

    print("got bank name: " +bank_name)
    
    if bank_name in allowed_bank_names:
        status_code = 200
        res = bank_name +" bank is allowed"
        logger.info(f"{bank_name} is allowed")
    else:
        status_code = 400
        res = bank_name +" bank is NOT allowed"
        logger.error(f"{bank_name} is NOT allowed")

    return return_response(res, status_code)


def lambda_handler(event, context):
    handler_log = 'lambda_handler: Received event=' + str(event) + " with event body=" + str(event.get("body"))
    logger.info(handler_log)

    return get_bank_info(event, context)
