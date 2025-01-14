package vn.sesgroup.hddt.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="DMPaymentType")
public class DMPaymentType {
    @Id
    private String id;
    private String code;
    private String name;
    private String IsDelete;
    private String order;

    public DMPaymentType(){}

    public DMPaymentType(String code, String name, String isDelete, String order) {
        this.code = code;
        this.name = name;
        IsDelete = isDelete;
        this.order = order;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIsDelete() {
        return IsDelete;
    }

    public void setIsDelete(String isDelete) {
        IsDelete = isDelete;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    @Override
    public String toString() {
        return "DMPaymentType{" +
                "id='" + id + '\'' +
                ", code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", IsDelete='" + IsDelete + '\'' +
                ", order='" + order + '\'' +
                '}';
    }
}
