package com.baskaaleksander.smartdocflowbackend.modules.testsupport.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TestUser {
    String email;
    String username;
    String rawPassword;
}
