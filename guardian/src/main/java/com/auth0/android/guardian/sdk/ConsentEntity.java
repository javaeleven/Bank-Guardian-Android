package com.auth0.android.guardian.sdk;

import java.util.List;
import java.util.Map;

public class ConsentEntity {
    public String tenant;
    public String client_id;
    public String audience;
    public List<Map<String, Object>> authorization_details;
}
