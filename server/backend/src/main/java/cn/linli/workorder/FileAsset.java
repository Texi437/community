package cn.linli.workorder;

// Storage key stays internal; API responses expose only authenticated download URLs.
public record FileAsset(String id, String workOrderId, String storageKey, String contentType, long byteSize) { }
