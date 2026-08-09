package org.example.web.stock.common.service;

import org.example.web.dao.GenericDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CIMapperTest {

    @Mock
    GenericDao genericDao;

    @Test
    void convertCodeToId_該当IDが見つかったときそのIDを返すこと() {
        when(genericDao.getIdByCode("companies", "7203")).thenReturn(42);
        CIMapper ciMapper = new CIMapper(genericDao);

        Integer id = ciMapper.convertCodeToId("companies", "7203");

        assertThat(id).isEqualTo(42);
    }

    @Test
    void convertCodeToId_該当IDが無いとき例外を投げること() {
        when(genericDao.getIdByCode("companies", "9999")).thenReturn(null);
        CIMapper ciMapper = new CIMapper(genericDao);

        assertThatThrownBy(() -> ciMapper.convertCodeToId("companies", "9999"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("9999")
                .hasMessageContaining("companies");
    }
}
