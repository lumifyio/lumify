package io.lumify.gdelt;

public class GDELTActor {
    private final String code;
    private final String name;
    private final String countryCode;
    private final String knownGroupCode;
    private final String ethnicCode;
    private final String religion1Code;
    private final String religion2Code;
    private final String type1Code;
    private final String type2Code;
    private final String type3Code;


    public GDELTActor(String code,
                      String name,
                      String countryCode,
                      String knownGroupCode,
                      String ethnicCode,
                      String religion1Code,
                      String religion2Code,
                      String type1Code,
                      String type2Code,
                      String type3Code) {
        this.code = code;
        this.name = name;
        this.countryCode = countryCode;
        this.knownGroupCode = knownGroupCode;
        this.ethnicCode = ethnicCode;
        this.religion1Code = religion1Code;
        this.religion2Code = religion2Code;
        this.type1Code = type1Code;
        this.type2Code = type2Code;
        this.type3Code = type3Code;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getKnownGroupCode() {
        return knownGroupCode;
    }

    public String getEthnicCode() {
        return ethnicCode;
    }

    public String getReligion1Code() {
        return religion1Code;
    }

    public String getReligion2Code() {
        return religion2Code;
    }

    public String getType1Code() {
        return type1Code;
    }

    public String getType2Code() {
        return type2Code;
    }

    public String getType3Code() {
        return type3Code;
    }

    public String getId() {
        String code = this.code != null ? this.code : "000";
        String name = this.name != null ? this.name : "UNK";
        String countryCode = this.countryCode != null ? this.countryCode : "000";
        String knownGroupCode = this.knownGroupCode != null ? this.knownGroupCode : "000";
        String ethnicCode = this.ethnicCode != null ? this.ethnicCode : "000";
        String religion1Code = this.religion1Code != null ? this.religion1Code : "000";
        String religion2Code = this.religion2Code != null ? this.religion2Code : "000";
        String type1Code = this.type1Code != null ? this.type1Code : "000";
        String type2Code = this.type2Code != null ? this.type2Code : "000";
        String type3Code = this.type3Code != null ? this.type3Code : "000";
        return code + name + countryCode + knownGroupCode + ethnicCode + religion1Code + religion2Code + type1Code + type2Code + type3Code;
    }
}
