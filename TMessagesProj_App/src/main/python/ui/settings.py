"""ui.settings (IthaGram) - elementos de la pantalla de ajustes del plugin."""
from dataclasses import dataclass


@dataclass
class Header:
    text: str
    type: str = "header"


@dataclass
class Divider:
    text: str = None
    type: str = "divider"


@dataclass
class Switch:
    key: str
    text: str
    default: bool
    subtext: str = None
    icon: str = None
    on_change: object = None
    on_long_click: object = None
    link_alias: str = None
    type: str = "switch"


@dataclass
class Selector:
    key: str
    text: str
    default: int
    items: list = None
    icon: str = None
    on_change: object = None
    on_long_click: object = None
    link_alias: str = None
    type: str = "selector"


@dataclass
class Input:
    key: str
    text: str
    default: str = ""
    subtext: str = None
    icon: str = None
    on_change: object = None
    on_long_click: object = None
    link_alias: str = None
    type: str = "input"


@dataclass
class Text:
    text: str
    subtext: str = None
    icon: str = None
    accent: bool = False
    red: bool = False
    on_click: object = None
    on_long_click: object = None
    create_sub_fragment: object = None
    link_alias: str = None
    type: str = "text"


@dataclass
class EditText:
    key: str
    hint: str
    default: str = ""
    multiline: bool = False
    max_length: int = 256
    mask: str = None
    on_change: object = None
    type: str = "edit_text"


@dataclass
class Custom:
    item: object = None
    view: object = None
    factory: object = None
    factory_args: object = None
    on_click: object = None
    on_long_click: object = None
    create_sub_fragment: object = None
    link_alias: str = None
    type: str = "custom"


class _FactoryInstance:
    def __init__(self, ssf):
        self._ssf = ssf
        self.java = ssf  # la pantalla de ajustes (etapa siguiente) usa estos callbacks


class SimpleSettingFactory:
    def __init__(self, create_view, bind_view, *, is_clickable=False, is_shadow=False,
                 create_item=None, on_click=None, on_long_click=None,
                 attached_view=None, equals=None, content_equals=None):
        self.create_view = create_view
        self.bind_view = bind_view
        self.is_clickable = is_clickable
        self.is_shadow = is_shadow
        self.create_item = create_item
        self.on_click = on_click
        self.on_long_click = on_long_click
        self.attached_view = attached_view
        self.equals = equals
        self.content_equals = content_equals
        self.instance = _FactoryInstance(self)

    def __call__(self, *args, **kwargs):
        return Custom(factory=self, factory_args=list(args) if args else None,
                      link_alias=kwargs.get("link_alias"))

    def release(self):
        self.create_view = None
        self.bind_view = None
        self.create_item = None
        self.on_click = None
        self.on_long_click = None
        self.attached_view = None
        self.equals = None
        self.content_equals = None
