package com.mod.loan.common.enums;

public enum MerchantEnum {
    helibao(1, "helibao", "合利宝"),
    fuyou(2, "fuyou", "富友"),
    huiju(3, "huiju", "汇聚"),
    yeepay(4, "yeepay", "易宝"),
    CHANGJIE(5, "changjie", "畅捷"),;

    private Integer code;
    private String abbreviation;
    private String desc;

    MerchantEnum(Integer code, String abbreviation, String desc) {
        this.code = code;
        this.abbreviation = abbreviation;
        this.desc = desc;
    }

    public static String getDesc(Integer code) {
        for (MerchantEnum status : MerchantEnum.values()) {
            if (status.getCode().equals(code)) {
                return status.getDesc();
            }
        }
        return null;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public String getAbbreviation() {
        return abbreviation;
    }
}
