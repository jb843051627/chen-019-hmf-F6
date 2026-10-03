package com.fc.v2.hmf;

import java.io.Serializable;

import com.fc.v2.mapper.auto.THmfAcctStepLogMapper;
import com.fc.v2.model.auto.THmfAcctStepLog;

/** 分户账立户单迈步痕迹内存假 Mapper。 */
class FakeAcctStepLogMapper extends InMemoryMapper<THmfAcctStepLog> implements THmfAcctStepLogMapper {

    @Override
    Serializable idOf(THmfAcctStepLog t) {
        return t.getId();
    }

    @Override
    void assignId(THmfAcctStepLog t, long id) {
        t.setId(id);
    }
}
