package com.btcassistant.opportunity;

/** Qualidade da evidência usada no score — cai para LIMITED sempre que algum componente estiver UNKNOWN (SDD §24). */
public enum EvidenceQuality {
    FULL,
    LIMITED
}
