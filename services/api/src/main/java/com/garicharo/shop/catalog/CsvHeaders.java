package com.garicharo.shop.catalog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class CsvHeaders {

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("name", "name"),
            Map.entry("product name", "name"),
            Map.entry("product_name", "name"),
            Map.entry("title", "name"),
            Map.entry("sku", "sku"),
            Map.entry("product code", "sku"),
            Map.entry("product_code", "sku"),
            Map.entry("code", "sku"),
            Map.entry("description", "description"),
            Map.entry("desc", "description"),
            Map.entry("category", "category"),
            Map.entry("cat", "category"),
            Map.entry("price", "price"),
            Map.entry("stock", "stock"),
            Map.entry("qty", "stock"),
            Map.entry("quantity", "stock"),
            Map.entry("weight_kg", "weight_kg"),
            Map.entry("weight kg", "weight_kg"),
            Map.entry("weightkg", "weight_kg"),
            Map.entry("weight", "weight_kg"));

    private CsvHeaders() {
    }

    static byte[] stripBom(byte[] bytes) {
        if (bytes != null
                && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            byte[] without = new byte[bytes.length - 3];
            System.arraycopy(bytes, 3, without, 0, without.length);
            return without;
        }
        return bytes;
    }

    static boolean looksLikeSpreadsheet(String filename, String contentType, byte[] bytes) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (name.endsWith(".xlsx") || name.endsWith(".xls") || name.endsWith(".ods")) {
            return true;
        }
        if (type.contains("spreadsheetml") || type.contains("opendocument.spreadsheet")) {
            return true;
        }
        if (bytes != null && bytes.length >= 4 && !name.endsWith(".csv")) {
            boolean zip = bytes[0] == 'P' && bytes[1] == 'K';
            boolean ole = (bytes[0] & 0xFF) == 0xD0 && (bytes[1] & 0xFF) == 0xCF;
            return zip || ole;
        }
        return false;
    }

    static String requiredColumnsMessage() {
        return "Need a UTF-8 comma-separated CSV whose header includes: "
                + String.join(", ", CsvRowValidator.requiredColumns())
                + ". Excel files (.xlsx) are not accepted; export as CSV. Extra columns are ignored. "
                + "SKU / product code / code map to sku; title / product name map to name.";
    }

    static Map<String, String> mapCanonicalToOriginal(List<String> rawHeaders) {
        Map<String, String> canonicalToOriginal = new LinkedHashMap<>();
        List<String> duplicateCanonical = new ArrayList<>();
        for (String raw : rawHeaders) {
            String canonical = canonicalName(raw);
            if (canonical == null) {
                continue;
            }
            if (canonicalToOriginal.containsKey(canonical)) {
                duplicateCanonical.add(canonical);
                continue;
            }
            canonicalToOriginal.put(canonical, raw);
        }
        if (!duplicateCanonical.isEmpty()) {
            return Map.of();
        }
        if (!canonicalToOriginal.keySet().containsAll(CsvRowValidator.requiredColumns())) {
            return Map.of();
        }
        return canonicalToOriginal;
    }

    static String canonicalName(String raw) {
        if (raw == null) {
            return null;
        }
        String key = stripBomString(raw).trim().toLowerCase(Locale.ROOT);
        key = key.replace('-', ' ').replace('_', ' ').replaceAll("\\s+", " ").trim();
        String spaced = key;
        String underscored = key.replace(' ', '_');
        if (ALIASES.containsKey(spaced)) {
            return ALIASES.get(spaced);
        }
        return ALIASES.get(underscored);
    }

    private static String stripBomString(String value) {
        if (value != null && !value.isEmpty() && value.charAt(0) == '\uFEFF') {
            return value.substring(1);
        }
        return value;
    }
}
