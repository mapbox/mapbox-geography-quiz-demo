package com.mapbox.tappergeochallenge;

/**
 * @
 */
public class EndpointException extends Exception {
  Endpoint _endpoint;

  public EndpointException(Endpoint endpoint, String message) {
    super(message);
  }

  public String getMessage() {
    return
      //todo
      //"Error query  : " ._endpoint.getQuery()."\n" .
      //  "Error endpoint: " ._endpoint.getEn."\n" .
      //  "Error http_response_code: " .$httpcode."\n" .
      //  "Error message: " .$response."\n";
      //  "Error data: " .print_r($data,true)."\n";
      //+
      super.getMessage();
  }
}