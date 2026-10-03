package com.fc.v2.hmf;

import java.io.Serializable;
import com.fc.v2.mapper.auto.THmfAcctCardLogMapper;
import com.fc.v2.model.auto.THmfAcctCardLog;

/** 分户账立户单步子留痕内存假 Mapper。 */
class FakeAcctCardLogMapper extends InMemoryMapper<THmfAcctCardLog> implements THmfAcctCardLogMapper {

    @Override
    Serializable idOf(THmfAcctCardLog t) {
        return t.getId();
    }

    @Override
    void assignId(THmfAcctCardLog t, long id) {
        t.setId(id);
    }
}
