package com.javeme.duobao.service;

import com.javeme.duobao.dto.AddressBookDTO;
import com.javeme.duobao.entity.AddressBook;
import com.javeme.duobao.repository.AddressBookRepository;
import com.javeme.duobao.vo.AddressBookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressBookService {

    private final AddressBookRepository addressBookRepository;

    public void add(Long userId, AddressBookDTO addressBookDTO) {

        AddressBook addressBook = new AddressBook();
        //if dto isDefault == 1, reset other address isDefault to 0, so isDefault = 1 only got 1
        if (addressBookDTO.getIsDefault() == 1) {
            addressBookRepository.resetDefault(userId);
        }
        addressBook.setUserId(userId);
        BeanUtils.copyProperties(addressBookDTO, addressBook);
        addressBookRepository.save(addressBook);
    }

    public void update(Long userId, Long addressBookId, AddressBookDTO addressBookDTO) {
        AddressBook addressBook = addressBookRepository.findById(addressBookId).orElseThrow(() ->
                new RuntimeException("Address not found"));

        if (!addressBook.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this address");
        }

        //if dto isDefault == 1, reset other address isDefault to 0, so isDefault = 1 only got 1
        if (addressBookDTO.getIsDefault() == 1) {
            addressBookRepository.resetDefault(userId);
        }

        BeanUtils.copyProperties(addressBookDTO, addressBook);
        addressBook.setId(addressBookId);
        addressBook.setUserId(userId);
        addressBookRepository.save(addressBook);
    }

    public List<AddressBookVO> list(Long userId) {
        List<AddressBook> list = addressBookRepository.findByUserIdOrderByIsDefaultDesc(userId);
        return list.stream()
                .map(this::convertToVO)
                .toList();
    }

    private AddressBookVO convertToVO(AddressBook addressBook) {
        AddressBookVO vo = new AddressBookVO();
        BeanUtils.copyProperties(addressBook, vo);
        return vo;
    }
}
