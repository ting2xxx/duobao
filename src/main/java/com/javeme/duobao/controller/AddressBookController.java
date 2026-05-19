package com.javeme.duobao.controller;

import com.javeme.duobao.common.BaseContext;
import com.javeme.duobao.dto.AddressBookDTO;
import com.javeme.duobao.entity.AddressBook;
import com.javeme.duobao.service.AddressBookService;
import com.javeme.duobao.vo.AddressBookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/address-books")
public class AddressBookController {

    private final AddressBookService addressBookService;

    /**
     * Add a new address
     * @param addressBookDTO
     * @return
     */
    @PostMapping("/add")
    public ResponseEntity<String> add(@RequestBody AddressBookDTO addressBookDTO) {
        Long userId = BaseContext.getCurrentID();
        addressBookService.add(userId, addressBookDTO);
        return ResponseEntity.ok("Address successfully added!");
    }

    /**
     * Update an existing address
     * @param addressBookId
     * @param addressBookDTO
     * @return
     */
    @PutMapping("/update/{addressBookId}")
    public ResponseEntity<String> update(@PathVariable Long addressBookId,
                                                @RequestBody AddressBookDTO addressBookDTO) {
        Long userId = BaseContext.getCurrentID();
        addressBookService.update(userId, addressBookId, addressBookDTO);
        return ResponseEntity.ok("Address successfully updated");
    }

    /**
     * list the addressBook
     * @return
     */
    @GetMapping("/list")
    public ResponseEntity<List<AddressBookVO>> list() {
        Long userId = BaseContext.getCurrentID();
        List<AddressBookVO> addressBookVOList = addressBookService.list(userId);

        return ResponseEntity.ok(addressBookVOList);
    }
}
