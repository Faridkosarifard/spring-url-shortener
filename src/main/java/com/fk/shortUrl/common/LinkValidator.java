package com.fk.shortUrl.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Short-Url
 * Created by Farid Kosarifard
 * Date: 10/23/2023
 * Time: 5:31 PM
 */
public class LinkValidator {
    private static final LinkValidator LINK_VALIDATOR= new LinkValidator();

    public LinkValidator() {
    }

    public static LinkValidator getInstance() {
        return LINK_VALIDATOR;
    }

    public  boolean isLinkValid(String input) {
        String linkPattern = "^(http|https?|ftp)://([A-Za-z0-9.-]+)+\\.[A-Za-z]+(:[0-9]+)?(/[A-Za-z0-9/_.-]+)*(\\?[A-Za-z0-9_./-]+)?(#\\w+)?$";
        Pattern pattern = Pattern.compile(linkPattern);
        Matcher matcher = pattern.matcher(input);

        return matcher.matches();
    }


}
