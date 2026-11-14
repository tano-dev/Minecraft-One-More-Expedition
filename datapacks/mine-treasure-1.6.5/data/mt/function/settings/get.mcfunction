$dialog show @s { \
  "type": "minecraft:server_links",\
  "title": {\
    "text": "Mine Treasure Settings"\
  },\
  "inputs": [\
    {\
      "type": "minecraft:single_option",\
      "key": "1",\
      "label": {\
        "text": "Progression",\
        "type": "text"\
      },\
      "options": $(1) \
    },\
    {\
      "type": "minecraft:single_option",\
      "key": "2",\
      "label": {\
        "text": "Spawn Frequency",\
        "type": "text"\
      },\
      "options": $(2) \
    },\
    $(3),\
    {\
      "type": "minecraft:single_option",\
      "key": "4",\
      "label": {\
        "text": "Despawn Speed",\
        "type": "text"\
      },\
      "options": $(4) \
    },\
    $(5),\
    $(6),\
    $(7),\
    $(8),\
    $(9),\
    $(10),\
    $(11),\
    $(12),\
    $(13),\
    $(14),\
    $(15),\
    $(16),\
    $(17)\
  ],\
  "pause": true,\
  "after_action": "close",\
  "exit_action": {\
    "label": {\
      "text": "Confirm"\
    },\
    "action": {\
      "type": "dynamic/run_command",\
      "template": "function mt:settings/set {1:\u0024(1),2:\u0024(2),3:\u0024(3),4:\u0024(4),5:\u0024(5),6:\u0024(6),7:\u0024(7),8:\u0024(8),9:\u0024(9),10:\u0024(10),11:\u0024(11),12:\u0024(12),13:\u0024(13),14:\u0024(14),15:\u0024(15),16:\u0024(16),17:\u0024(17)}"\
    }\
  }\
}
