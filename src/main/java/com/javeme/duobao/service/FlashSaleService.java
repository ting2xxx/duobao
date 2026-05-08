package com.javeme.duobao.service;

import com.javeme.duobao.dto.FlashSaleCreateDTO;
import com.javeme.duobao.dto.FlashSaleResponseDTO;
import com.javeme.duobao.entity.FlashSale;
import com.javeme.duobao.entity.Product;
import com.javeme.duobao.repository.FlashSaleRepository;
import com.javeme.duobao.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleRepository flashSaleRepository;
    private final ProductRepository productRepository;

    public Map<String,List<FlashSaleResponseDTO>> getCalendar(String startTime, String endTime) {
        //Convert string to LocalDateTime
        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        if (startTime == null || endTime == null) {

            LocalDateTime now = LocalDateTime.now();
            startDateTime = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            endDateTime = now.withDayOfMonth(
                    now.toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);

        } else {

            startDateTime = LocalDateTime.parse(startTime);
            endDateTime = LocalDateTime.parse(endTime);
        }

        List<FlashSale> list = flashSaleRepository.getCalendar(startDateTime, endDateTime);

        List<Long> productIds  = list.stream().map(FlashSale::getProductId)
                .toList();

        List<Product> products = productRepository.findAllByIdIn(productIds);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<FlashSaleResponseDTO> dtoList = list.stream()
                .map(sale -> {
                    FlashSaleResponseDTO dto = new FlashSaleResponseDTO();
                    dto.setFlashSaleId(sale.getId());
                    dto.setProductId(sale.getProductId());
                    dto.setFlashSalePrice(sale.getFlashSalePrice());
                    dto.setStartTime(sale.getStartTime());
                    dto.setEndTime(sale.getEndTime());
                    dto.setStatus(sale.getStatus());

                    Product product = productMap.get(sale.getProductId());
                    if (product != null) {
                        dto.setProductName(product.getProductName());
                        dto.setImage(product.getImage());
                        dto.setOriginalPrice(product.getPrice());
                    }

                    return dto;

                }).toList();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return dtoList.stream()
                .collect(Collectors.groupingBy(
                        dto -> dto.getStartTime().format(formatter)));
    }

    public void createFlashSale(FlashSaleCreateDTO flashSaleCreateDTO) {
        FlashSale flashSale = new FlashSale();
        flashSale.setProductId(flashSaleCreateDTO.getProductId());
        flashSale.setFlashSalePrice(flashSaleCreateDTO.getFlashSalePrice());
        flashSale.setStartTime(flashSaleCreateDTO.getStartTime());
        flashSale.setEndTime(flashSaleCreateDTO.getEndTime());
        flashSale.setStatus(0);

        flashSaleRepository.save(flashSale);
    }
}
