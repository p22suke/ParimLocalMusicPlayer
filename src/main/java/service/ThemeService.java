package service;

import model.ThemeMode;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Small theme helper.
 *
 * AUTO mode tries to use sunrise/sunset if latitude and longitude are provided
 * through JVM system properties:
 * -Dparim.latitude=59.4370 -Dparim.longitude=24.7536
 *
 * If no coordinates are provided, it falls back to a simple 07:00-19:00 day
 * range so the MVP still behaves predictably on both macOS and Windows.
 */
public final class ThemeService {
    public String stylesheetFor(ThemeMode mode) {
        ThemeMode resolved = resolve(mode);
        return switch (resolved) {
            case DAY -> "/styles/day.css";
            case NIGHT -> "/styles/night.css";
            case AUTO -> "/styles/day.css";
        };
    }

    public ThemeMode resolve(ThemeMode mode) {
        if (mode != ThemeMode.AUTO) {
            return mode;
        }
        return isNightNow() ? ThemeMode.NIGHT : ThemeMode.DAY;
    }

    public boolean isNightNow() {
        Double latitude = getDoubleProperty("parim.latitude");
        Double longitude = getDoubleProperty("parim.longitude");
        LocalTime now = LocalTime.now();

        if (latitude == null || longitude == null) {
            return now.isBefore(LocalTime.of(7, 0)) || now.isAfter(LocalTime.of(19, 0));
        }

        SunTimes sunTimes = calculateSunTimes(LocalDate.now(), latitude, longitude, ZoneId.systemDefault());
        return now.isBefore(sunTimes.sunrise()) || now.isAfter(sunTimes.sunset());
    }

    private Double getDoubleProperty(String key) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private SunTimes calculateSunTimes(LocalDate date, double latitude, double longitude, ZoneId zoneId) {
        // NOAA-inspired approximation. Accurate enough for a visual theme toggle.
        int dayOfYear = date.getDayOfYear();
        double lngHour = longitude / 15.0;
        double sunriseUtc = calculateUtcHour(dayOfYear, latitude, lngHour, true);
        double sunsetUtc = calculateUtcHour(dayOfYear, latitude, lngHour, false);

        ZonedDateTime sunrise = date.atStartOfDay(zoneId).withZoneSameInstant(ZoneId.of("UTC"))
                .plusMinutes(Math.round(sunriseUtc * 60));
        ZonedDateTime sunset = date.atStartOfDay(zoneId).withZoneSameInstant(ZoneId.of("UTC"))
                .plusMinutes(Math.round(sunsetUtc * 60));

        return new SunTimes(sunrise.withZoneSameInstant(zoneId).toLocalTime(), sunset.withZoneSameInstant(zoneId).toLocalTime());
    }

    private double calculateUtcHour(int dayOfYear, double latitude, double lngHour, boolean sunrise) {
        double t = dayOfYear + ((sunrise ? 6 : 18) - lngHour) / 24;
        double meanAnomaly = (0.9856 * t) - 3.289;
        double trueLongitude = meanAnomaly
                + (1.916 * Math.sin(Math.toRadians(meanAnomaly)))
                + (0.020 * Math.sin(Math.toRadians(2 * meanAnomaly)))
                + 282.634;
        trueLongitude = normalizeAngle(trueLongitude);

        double rightAscension = Math.toDegrees(Math.atan(0.91764 * Math.tan(Math.toRadians(trueLongitude))));
        rightAscension = normalizeAngle(rightAscension);

        double lQuadrant = Math.floor(trueLongitude / 90) * 90;
        double raQuadrant = Math.floor(rightAscension / 90) * 90;
        rightAscension = (rightAscension + (lQuadrant - raQuadrant)) / 15;

        double sinDeclination = 0.39782 * Math.sin(Math.toRadians(trueLongitude));
        double cosDeclination = Math.cos(Math.asin(sinDeclination));
        double cosHourAngle = (Math.cos(Math.toRadians(90.833))
                - (sinDeclination * Math.sin(Math.toRadians(latitude))))
                / (cosDeclination * Math.cos(Math.toRadians(latitude)));

        if (cosHourAngle < -1 || cosHourAngle > 1) {
            return sunrise ? 7 : 19;
        }

        double hourAngle = sunrise
                ? 360 - Math.toDegrees(Math.acos(cosHourAngle))
                : Math.toDegrees(Math.acos(cosHourAngle));
        hourAngle /= 15;

        double localMeanTime = hourAngle + rightAscension - (0.06571 * t) - 6.622;
        double utcTime = localMeanTime - lngHour;
        return (utcTime % 24 + 24) % 24;
    }

    private double normalizeAngle(double degrees) {
        return (degrees % 360 + 360) % 360;
    }

    private record SunTimes(LocalTime sunrise, LocalTime sunset) {
    }
}
