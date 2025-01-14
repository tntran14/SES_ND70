package vn.sesgroup.hddt.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Data
@Document(collection = "Issuer")
public class Issuer {

    @Id
    private String id;
    private String AgentId;
    private String TaxCode;

    public Issuer(){}

    public Issuer(String id, String agentId, String taxCode) {
        this.id = id;
        AgentId = agentId;
        TaxCode = taxCode;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAgentId() {
        return AgentId;
    }

    public void setAgentId(String agentId) {
        AgentId = agentId;
    }

    public String getTaxCode() {
        return TaxCode;
    }

    public void setTaxCode(String taxCode) {
        TaxCode = taxCode;
    }
}
