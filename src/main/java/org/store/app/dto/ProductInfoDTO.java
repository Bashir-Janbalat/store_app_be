package org.store.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Product information details")
public class ProductInfoDTO {

    @Schema(description = "Name of the product", example = "Wireless Headphones")
    private String name;

    @Schema(description = "Description of the product", example = "High quality wireless headphones with noise cancellation")
    private String description;

    @Schema(description = "URL of the product image", example = "https://example.com/images/product123.jpg")
    private String imageUrl;

    @Schema(description = "Total stock of the product", example = "100")
    private Long totalStock;

    public ProductInfoDTO(String name, String description, String imageUrl) {
        this.name = name;
        this.description = description;
        this.imageUrl = imageUrl;
    }
}
