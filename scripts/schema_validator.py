import re
from datetime import datetime

class ValidationError(Exception):
    def __init__(self, message, path="root"):
        super().__init__(f"{path}: {message}")
        self.path = path
        self.message = message

def validate_iso8601_datetime(value):
    if not isinstance(value, str):
        return False
    # ISO-8601 regex pattern
    pattern = r'^\d{4}-\d{2}-\d{2}[Tt]\d{2}:\d{2}:\d{2}(\.\d+)?([Zz]|([+-]\d{2}:\d{2}))?$'
    if not re.match(pattern, value):
        return False
    try:
        # Basic calendar sanity check
        base_part = value[:19].replace('T', ' ').replace('t', ' ')
        datetime.strptime(base_part, "%Y-%m-%d %H:%M:%S")
        return True
    except Exception:
        return False

def validate_schema(instance, schema, path="root"):
    """Validates an instance against a JSON Schema (Draft-07)."""
    if not isinstance(schema, dict):
        return

    # Check type
    if "type" in schema:
        expected_type = schema["type"]
        if expected_type == "object":
            if not isinstance(instance, dict):
                raise ValidationError(f"expected object, got {type(instance).__name__}", path)
        elif expected_type == "array":
            if not isinstance(instance, list):
                raise ValidationError(f"expected array, got {type(instance).__name__}", path)
        elif expected_type == "string":
            if not isinstance(instance, str):
                raise ValidationError(f"expected string, got {type(instance).__name__}", path)
        elif expected_type == "integer":
            if not isinstance(instance, int) or isinstance(instance, bool):
                raise ValidationError(f"expected integer, got {type(instance).__name__}", path)
        elif expected_type == "number":
            if not isinstance(instance, (int, float)) or isinstance(instance, bool):
                raise ValidationError(f"expected number, got {type(instance).__name__}", path)
        elif expected_type == "boolean":
            if not isinstance(instance, bool):
                raise ValidationError(f"expected boolean, got {type(instance).__name__}", path)
        elif expected_type == "null":
            if instance is not None:
                raise ValidationError(f"expected null, got {type(instance).__name__}", path)

    # Check const
    if "const" in schema:
        if instance != schema["const"]:
            raise ValidationError(f"value {repr(instance)} does not match const {repr(schema['const'])}", path)

    # Check enum
    if "enum" in schema:
        if instance not in schema["enum"]:
            raise ValidationError(f"value {repr(instance)} not in enum {schema['enum']}", path)

    # Check string constraints
    if isinstance(instance, str):
        if "minLength" in schema and len(instance) < schema["minLength"]:
            raise ValidationError(f"string length {len(instance)} < minLength {schema['minLength']}", path)
        if "maxLength" in schema and len(instance) > schema["maxLength"]:
            raise ValidationError(f"string length {len(instance)} > maxLength {schema['maxLength']}", path)
        if "pattern" in schema:
            if not re.search(schema["pattern"], instance):
                raise ValidationError(f"string '{instance}' does not match pattern '{schema['pattern']}'", path)
        if schema.get("format") == "date-time":
            if not validate_iso8601_datetime(instance):
                raise ValidationError(f"string '{instance}' is not a valid ISO-8601 date-time", path)

    # Check number constraints
    if isinstance(instance, (int, float)) and not isinstance(instance, bool):
        if "minimum" in schema and instance < schema["minimum"]:
            raise ValidationError(f"value {instance} < minimum {schema['minimum']}", path)
        if "maximum" in schema and instance > schema["maximum"]:
            raise ValidationError(f"value {instance} > maximum {schema['maximum']}", path)

    # Check array constraints
    if isinstance(instance, list):
        if "minItems" in schema and len(instance) < schema["minItems"]:
            raise ValidationError(f"array items count {len(instance)} < minItems {schema['minItems']}", path)
        if "maxItems" in schema and len(instance) > schema["maxItems"]:
            raise ValidationError(f"array items count {len(instance)} > maxItems {schema['maxItems']}", path)
        if "items" in schema:
            item_schema = schema["items"]
            for idx, item in enumerate(instance):
                validate_schema(item, item_schema, f"{path}[{idx}]")

    # Check object constraints
    if isinstance(instance, dict):
        # required fields
        if "required" in schema:
            for req in schema["required"]:
                if req not in instance:
                    raise ValidationError(f"missing required property '{req}'", path)
        
        # properties validation
        properties = schema.get("properties", {})
        for prop, val in instance.items():
            if prop in properties:
                validate_schema(val, properties[prop], f"{path}.{prop}")
            elif schema.get("additionalProperties") is False:
                raise ValidationError(f"additional property '{prop}' is not allowed", path)
            elif isinstance(schema.get("additionalProperties"), dict):
                validate_schema(val, schema["additionalProperties"], f"{path}.{prop}")

    # Check allOf
    if "allOf" in schema:
        for idx, sub_schema in enumerate(schema["allOf"]):
            validate_schema(instance, sub_schema, f"{path}.allOf[{idx}]")

    # Check if / then / else (Draft-07 conditional validation)
    if "if" in schema:
        if_valid = True
        try:
            validate_schema(instance, schema["if"], f"{path}.if")
        except ValidationError:
            if_valid = False

        if if_valid:
            if "then" in schema:
                validate_schema(instance, schema["then"], f"{path}.then")
        else:
            if "else" in schema:
                validate_schema(instance, schema["else"], f"{path}.else")


# ==============================================================================


def check_schema_valid(instance, schema):
    try:
        validate_schema(instance, schema)
        return []
    except ValidationError as e:
        return [str(e)]
