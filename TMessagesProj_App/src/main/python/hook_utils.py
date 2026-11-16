"""hook_utils (IthaGram) - ayudas de reflexion Java."""
from java import jclass
from android_utils import log


class _ClassRef(object):
    """Respaldo: envuelve un jclass para ofrecer getClass() -> java.lang.Class."""

    def __init__(self, jc, name):
        self._jc = jc
        self._name = name

    def getClass(self):
        return jclass("java.lang.Class").forName(self._name)

    def __getattr__(self, item):
        return getattr(self._jc, item)


def find_class(class_name):
    """Devuelve la clase Java `class_name` o None si no existe."""
    if not class_name:
        return None
    try:
        cls = jclass(class_name)
    except Exception:
        return None
    try:
        cls.getClass().getName()
        return cls
    except Exception:
        return _ClassRef(cls, class_name)


def _as_class_object(clazz):
    if clazz is None:
        return None
    try:
        if hasattr(clazz, "getDeclaredField"):
            return clazz
    except Exception:
        pass
    try:
        return clazz.getClass()
    except Exception:
        return None


def _find_field(clazz, field_name):
    clazz = _as_class_object(clazz)
    if clazz is None or not field_name:
        return None
    current = clazz
    while current is not None:
        try:
            field = current.getDeclaredField(field_name)
            if field is not None:
                return field
        except Exception:
            pass
        try:
            current = current.getSuperclass()
        except Exception:
            current = None
    return None


def get_private_field(obj, field_name):
    try:
        if obj is None:
            return None
        field = _find_field(obj.getClass(), field_name)
        if field is None:
            return None
        field.setAccessible(True)
        return field.get(obj)
    except Exception:
        return None


def set_private_field(obj, field_name, new_value):
    try:
        if obj is None:
            return False
        field = _find_field(obj.getClass(), field_name)
        if field is None:
            return False
        field.setAccessible(True)
        field.set(obj, new_value)
        return True
    except Exception:
        return False


def get_static_private_field(clazz, field_name):
    try:
        field = _find_field(clazz, field_name)
        if field is None:
            return None
        field.setAccessible(True)
        return field.get(None)
    except Exception:
        return None


def set_static_private_field(clazz, field_name, new_value):
    try:
        field = _find_field(clazz, field_name)
        if field is None:
            return False
        field.setAccessible(True)
        field.set(None, new_value)
        return True
    except Exception:
        return False
