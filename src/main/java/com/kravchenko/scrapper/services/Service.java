package com.kravchenko.scrapper.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PRIVATE, force = true)
public final class Service {

    @JsonProperty("name")
    public final String name;

    @JsonProperty("url")
    public final String url;

}
