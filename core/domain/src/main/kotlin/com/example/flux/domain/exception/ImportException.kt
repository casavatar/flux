package com.example.flux.domain.exception

class UnsupportedFormatException(mimeType: String?) :
    Exception("Unsupported MIME type: $mimeType")

class PermissionException(uriString: String) :
    Exception("Failed to acquire persistable permission for: $uriString")
