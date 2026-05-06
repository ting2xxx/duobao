package com.javeme.duobao.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {

    // 1. The Receipt Number (Generated instantly for the user's screen)
    private String orderNumber;

    // 2. WHO is buying? (Extracted from BaseContext before sending)
    private Long userId;

    // 3. WHAT are they buying?
    private Long productId;
    private Integer quantity;

    // 4. WHERE is it going?
    private Long addressBookId;

    // 5. Any notes?
    private String remark;
}
