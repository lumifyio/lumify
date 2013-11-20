package com.altamiracorp.lumify.location;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.geoNames.*;
import com.altamiracorp.lumify.core.model.termMention.TermMention;
import com.google.inject.Inject;

import java.util.regex.Pattern;

public class SimpleTermLocationExtractor {
    private static final String POSTAL_CODE_REGEX = "^\\d{5}$|^\\d{5}-\\d{4}$"; //US zip code
    private static final Long POSTAL_CODE_POPULATION = 1000000L; // in the absence of population data, let's make it up!
    private GeoNameAdmin1CodeRepository geoNameAdmin1CodeRepository;
    private GeoNameCountryInfoRepository geoNameCountryInfoRepository;

    @Inject
    public SimpleTermLocationExtractor(GeoNameAdmin1CodeRepository geoNameAdmin1CodeRepository, GeoNameCountryInfoRepository geoNameCountryInfoRepository) {
        this.geoNameAdmin1CodeRepository = geoNameAdmin1CodeRepository;
        this.geoNameCountryInfoRepository = geoNameCountryInfoRepository;
    }

    public TermMention getTermWithLocationLookup(GeoNameRepository geoNameRepository, TermMention termMention, User user) {
        String sign = termMention.getMetadata().getSign();
        GeoName geoName = geoNameRepository.findBestMatch(sign, user);
        if (geoName == null) {
            return null;
        }

        return populateTermMentions(termMention,
                geoName.getMetadata().getLatitude(),
                geoName.getMetadata().getLongitude(),
                getTitleFromGeoName(geoName, user),
                geoName.getMetadata().getPopulation());
    }

    public TermMention getTermWithPostalCodeLookup(GeoNamePostalCodeRepository geoNamePostalCodeRepository, TermMention termMention, User user) {
        //we are assuming all US zip codes at this point!
        String zip = termMention.getMetadata().getSign().length() == 5 ? termMention.getMetadata().getSign() : termMention.getMetadata().getSign().substring(0, 5);
        GeoNamePostalCode postalCode = geoNamePostalCodeRepository.findByUSZipCode(zip, user);
        if (postalCode == null) {
            return null;
        }
        return populateTermMentions(termMention,
                postalCode.getMetadata().getLatitude(),
                postalCode.getMetadata().getLongitude(),
                getTitleFromPostalCode(postalCode),
                POSTAL_CODE_POPULATION);
    }

    private TermMention populateTermMentions(TermMention term, Double latitude, Double longitude, String title, Long population) {
        term.getMetadata().setGeoLocation(latitude, longitude);
        term.getMetadata().setGeoLocationTitle(title);
        term.getMetadata().setGeoLocationPopulation(population);
        return term;
    }

    private String getTitleFromGeoName(GeoName geoName, User user) {
        GeoNameAdmin1Code code = geoNameAdmin1CodeRepository.findByCountryAndAdmin1Code(geoName.getMetadata().getCountryCode(), geoName.getMetadata().getAdmin1Code(), user);
        GeoNameCountryInfo countryInfo = geoNameCountryInfoRepository.findByCountryCode(geoName.getMetadata().getCountryCode(), user);
        String countryString = geoName.getMetadata().getCountryCode();
        if (countryInfo != null) {
            countryString = countryInfo.getMetadata().getTitle();
        }

        if (code != null) {
            return geoName.getMetadata().getName() + ", " + code.getMetadata().getTitle() + ", " + countryString;
        } else {
            return geoName.getMetadata().getName() + ", " + countryString;
        }
    }

    private String getTitleFromPostalCode(GeoNamePostalCode postalCode) {
        StringBuilder sb = new StringBuilder(postalCode.getMetadata().getPlaceName());
        sb.append(", ")
                .append(postalCode.getMetadata().getAdmin1Code())
                .append(", ")
                .append(postalCode.getRowKey().getCountryCode())
                .append(" (")
                .append(postalCode.getRowKey().getPostalCode())
                .append(")");
        return sb.toString();
    }

    public boolean isPostalCode(TermMention termMention) {
        return isPostalCode(termMention.getMetadata().getSign());
    }

    private boolean isPostalCode(String location) {
        return Pattern.matches(POSTAL_CODE_REGEX, location);
    }
}
