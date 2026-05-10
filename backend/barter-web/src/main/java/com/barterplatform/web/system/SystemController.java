package com.barterplatform.web.system;

import com.barterplatform.api.controller.SystemApi;
import com.barterplatform.api.model.PingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController implements SystemApi {

    @Override
    public ResponseEntity<PingResponse> ping() {
        PingResponse response = new PingResponse();
        response.setMessage("pong");
        return ResponseEntity.ok(response);
    }
}

