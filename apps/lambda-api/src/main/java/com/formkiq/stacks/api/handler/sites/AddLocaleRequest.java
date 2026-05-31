package com.formkiq.stacks.api.handler.sites;

import com.formkiq.graalvm.annotations.Reflectable;

@Reflectable
public record AddLocaleRequest(String locale) {
}
