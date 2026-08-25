package com.openex.backend.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class DuplicateResourceException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.BAD_REQUEST)
class InvalidOrderException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
class InsufficientFundsException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.FORBIDDEN)
class UnauthorizedOperationException(message: String) : RuntimeException(message)

@ResponseStatus(HttpStatus.CONFLICT)
class IdempotencyConflictException(message: String) : RuntimeException(message)
