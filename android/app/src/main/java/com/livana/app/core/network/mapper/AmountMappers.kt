package com.livana.app.core.network.mapper

import com.livana.app.core.model.Usdc
import java.math.BigInteger

internal fun Long.toUsdc(): Usdc = Usdc(BigInteger.valueOf(this))
