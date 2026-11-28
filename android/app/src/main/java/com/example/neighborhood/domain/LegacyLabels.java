package com.example.neighborhood.domain;

public final class LegacyLabels {
  private static final String[] TOPIC_CODES = {"VOLUNTEERING", "READING", "FITNESS", "DANCE", "ENVIRONMENT", "MUTUAL_HELP"};
  private static final String[] TOPIC_NAMES = {"志愿服务", "阅读交流", "文体健身", "文艺舞蹈", "户外环保", "邻里互助"};
  private static final String[] CATEGORY_CODES = {"ENVIRONMENT", "FACILITY", "SECURITY", "NOISE", "OTHER"};
  private static final String[] CATEGORY_NAMES = {"环境卫生", "设施维修", "安全隐患", "噪声扰民", "其他事项"};

  private LegacyLabels() {}

  // Older local drafts store display names instead of API codes.
  public static String topicCode(String value) { return translate(value, TOPIC_NAMES, TOPIC_CODES); }
  public static String categoryCode(String value) {
    if ("公共设施".equals(value)) return "FACILITY";
    if ("社区秩序".equals(value)) return "SECURITY";
    if ("其他诉求".equals(value)) return "OTHER";
    return translate(value, CATEGORY_NAMES, CATEGORY_CODES);
  }
  public static String topic(String code) { return translate(code, TOPIC_CODES, TOPIC_NAMES); }
  public static String category(String code) { return translate(code, CATEGORY_CODES, CATEGORY_NAMES); }
  private static String translate(String value, String[] from, String[] to) {
    for (int i = 0; i < from.length; i++) if (from[i].equals(value)) return to[i];
    return value;
  }
}
