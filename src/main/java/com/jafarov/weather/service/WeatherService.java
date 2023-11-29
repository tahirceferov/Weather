package com.jafarov.weather.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jafarov.weather.dto.Location;
import com.jafarov.weather.dto.WeatherDto;
import com.jafarov.weather.dto.WeatherResponse;
import com.jafarov.weather.model.WeatherEntity;
import com.jafarov.weather.repository.WeatherRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
public class WeatherService {
    private static final String API_URL = "http://api.weatherstack.com/current?access_key=24cd29792697f1a13165c9de62498ee7&query=";
    private final WeatherRepository weatherRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WeatherService(WeatherRepository weatherRepository, RestTemplate restTemplate) {
        this.weatherRepository = weatherRepository;
        this.restTemplate = restTemplate;
    }

    public WeatherDto getWeatherByCityName(String city) {
        Optional<WeatherEntity> weatherEntityOptional
                = weatherRepository.findFirstByRequestedCityNameOrderByUpdatedTimeDesc(city);
        if (!weatherEntityOptional.isPresent()) {
            return WeatherDto.convert(getWeatherFromWeatherStack(city));
        }
        return WeatherDto.convert(weatherEntityOptional.get());
    }

    private WeatherEntity getWeatherFromWeatherStack(String city) {
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(API_URL + city, String.class);
        try {
            WeatherResponse weatherResponse = objectMapper.readValue(responseEntity.getBody(), WeatherResponse.class);
            return saveWeatherEntity(city, weatherResponse);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private WeatherEntity saveWeatherEntity(String city, WeatherResponse weatherResponse) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            WeatherEntity weatherEntity = new WeatherEntity(city,
                    weatherResponse.location().name(),
                    weatherResponse.location().country(),
                    weatherResponse.current().temperature(),
                    LocalDateTime.now(),
                    LocalDateTime.parse(weatherResponse.location().localTime(), dateTimeFormatter));
            return weatherRepository.save(weatherEntity);
        }
    }

