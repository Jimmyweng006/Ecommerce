package com.jimmyweng.ecommerce.controller.favorite.dto;

import jakarta.validation.constraints.NotNull;

public record AddFavoriteRequest(@NotNull Long productId) {}
