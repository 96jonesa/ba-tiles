package com.batiles;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
enum BARole
{
    ATTACKER("a", "Attacker"),
    COLLECTOR("c", "Collector"),
    DEFENDER("d", "Defender"),
    HEALER("h", "Healer");

    /**
     * The code stored in {@link GroundMarkerPoint#getRoles()}.
     */
    private final String code;
    private final String displayName;

    static BARole fromCode(String code)
    {
        for (BARole role : values())
        {
            if (role.code.equals(code))
            {
                return role;
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
