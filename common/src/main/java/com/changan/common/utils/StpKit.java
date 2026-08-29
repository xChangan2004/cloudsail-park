package com.changan.common.utils;

import cn.dev33.satoken.stp.StpLogic;
import com.changan.common.constants.Constants;

public class StpKit {

    public static final StpLogic ADMIN = new StpLogic(Constants.Admin.TYPE);

    public static final StpLogic APP = new StpLogic(Constants.App.TYPE);
}
