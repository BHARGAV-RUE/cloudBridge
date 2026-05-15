package com.bhargav.cloudbridge.util;

public final class StorageLimits {

    private StorageLimits() {}
        // 4_831_838_208L -> 4.5GB
        // 9_663_676_416L -> 9GB
        // 161_073_741_824L  -> 150GB
    public static final Long AWS_LIMIT_BYTES = 4_831_838_208L;
    public static final Long AZURE_LIMIT_BYTES = 4_831_838_208L;
    public static final Long ORACLE_LIMIT_BYTES = 9_663_676_416L;
    public static final Long GOOGLE_DRIVE_LIMIT_BYTES = 161_073_741_824L ;
    //public static final Long BLACKBLAZE_LIMIT_BYTES = 4_831_838_208L;
}
