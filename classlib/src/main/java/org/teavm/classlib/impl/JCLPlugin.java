/*
 *  Copyright 2014 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.classlib.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import org.teavm.backend.c.TeaVMCHost;
import org.teavm.backend.javascript.TeaVMJavaScriptHost;
import org.teavm.backend.wasm.TeaVMWasmHost;
import org.teavm.classlib.ReflectionSupplier;
import org.teavm.classlib.impl.currency.CountriesGenerator;
import org.teavm.classlib.impl.currency.CurrenciesGenerator;
import org.teavm.classlib.impl.currency.CurrencyHelper;
import org.teavm.classlib.impl.lambda.LambdaMetafactorySubstitutor;
import org.teavm.classlib.impl.tz.DateTimeZoneProvider;
import org.teavm.classlib.impl.tz.DateTimeZoneProviderIntrinsic;
import org.teavm.classlib.impl.tz.DateTimeZoneProviderPatch;
import org.teavm.classlib.impl.tz.TimeZoneGenerator;
import org.teavm.classlib.impl.unicode.AvailableLocalesMetadataGenerator;
import org.teavm.classlib.impl.unicode.CLDRHelper;
import org.teavm.classlib.impl.unicode.CLDRReader;
import org.teavm.classlib.impl.unicode.CountryMetadataGenerator;
import org.teavm.classlib.impl.unicode.CurrencyLocalizationMetadataGenerator;
import org.teavm.classlib.impl.unicode.DateFormatMetadataGenerator;
import org.teavm.classlib.impl.unicode.DateSymbolsMetadataGenerator;
import org.teavm.classlib.impl.unicode.DecimalMetadataGenerator;
import org.teavm.classlib.impl.unicode.DefaultLocaleMetadataGenerator;
import org.teavm.classlib.impl.unicode.LanguageMetadataGenerator;
import org.teavm.classlib.impl.unicode.LikelySubtagsMetadataGenerator;
import org.teavm.classlib.impl.unicode.NumberFormatMetadataGenerator;
import org.teavm.classlib.impl.unicode.TimeZoneLocalizationGenerator;
import org.teavm.classlib.java.lang.CharacterMetadataGenerator;
import org.teavm.classlib.java.lang.reflect.AnnotationDependencyListener;
import org.teavm.interop.PlatformMarker;
import org.teavm.model.MethodReference;
import org.teavm.model.ValueType;
import org.teavm.platform.PlatformClass;
import org.teavm.platform.metadata.ResourceArray;
import org.teavm.platform.metadata.ResourceMap;
import org.teavm.platform.metadata.StringResource;
import org.teavm.platform.plugin.MetadataRegistration;
import org.teavm.platform.plugin.PlatformPlugin;
import org.teavm.vm.TeaVMPluginUtil;
import org.teavm.vm.spi.After;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

@After(PlatformPlugin.class)
public class JCLPlugin implements TeaVMPlugin {
    @Override
    public void install(TeaVMHost host) {
        host.add(new ObfuscationHacks());

        if (!isBootstrap()) {
            ServiceLoaderSupport serviceLoaderSupport = new ServiceLoaderSupport(host.getClassLoader());
            host.add(serviceLoaderSupport);
            host.registerService(ServiceLoaderInformation.class, serviceLoaderSupport);
            MethodReference loadServicesMethod = new MethodReference(ServiceLoader.class, "loadServices",
                    PlatformClass.class, Object[].class);

            TeaVMJavaScriptHost jsExtension = host.getExtension(TeaVMJavaScriptHost.class);
            if (jsExtension != null) {
                jsExtension.add(loadServicesMethod, new ServiceLoaderJSSupport());
                jsExtension.addVirtualMethods(new AnnotationVirtualMethods());
            }

            TeaVMCHost cHost = host.getExtension(TeaVMCHost.class);
            if (cHost != null) {
                cHost.addGenerator(new ServiceLoaderCSupport());
            }
        }

        if (!isBootstrap()) {
            host.registerService(CLDRReader.class, CLDRReader.getInstance(host.getProperties(), host.getClassLoader()));

            host.add(new ClassForNameTransformer());
        }

        host.add(new AnnotationDependencyListener());

        LambdaMetafactorySubstitutor lms = new LambdaMetafactorySubstitutor();
        host.add(new MethodReference("java.lang.invoke.LambdaMetafactory", "metafactory",
                ValueType.object("java.lang.invoke.MethodHandles$Lookup"), ValueType.object("java.lang.String"),
                ValueType.object("java.lang.invoke.MethodType"), ValueType.object("java.lang.invoke.MethodType"),
                ValueType.object("java.lang.invoke.MethodHandle"), ValueType.object("java.lang.invoke.MethodType"),
                ValueType.object("java.lang.invoke.CallSite")), lms);
        host.add(new MethodReference("java.lang.invoke.LambdaMetafactory", "altMetafactory",
                ValueType.object("java.lang.invoke.MethodHandles$Lookup"),
                ValueType.object("java.lang.String"), ValueType.object("java.lang.invoke.MethodType"),
                ValueType.arrayOf(ValueType.object("java.lang.Object")),
                ValueType.object("java.lang.invoke.CallSite")), lms);

        StringConcatFactorySubstitutor stringConcatSubstitutor = new StringConcatFactorySubstitutor();
        host.add(new MethodReference("java.lang.invoke.StringConcatFactory", "makeConcat",
                ValueType.object("java.lang.invoke.MethodHandles$Lookup"), ValueType.object("java.lang.String"),
                ValueType.object("java.lang.invoke.MethodType"), ValueType.object("java.lang.invoke.CallSite")),
                stringConcatSubstitutor);
        host.add(new MethodReference("java.lang.invoke.StringConcatFactory", "makeConcatWithConstants",
                        ValueType.object("java.lang.invoke.MethodHandles$Lookup"), ValueType.object("java.lang.String"),
                        ValueType.object("java.lang.invoke.MethodType"), ValueType.object("java.lang.String"),
                        ValueType.arrayOf(ValueType.object("java.lang.Object")),
                        ValueType.object("java.lang.invoke.CallSite")),
                stringConcatSubstitutor);

        if (!isBootstrap()) {
            host.add(new ScalaHacks());
            host.add(new KotlinHacks());
        }

        host.add(new NumericClassTransformer());
        host.add(new SystemClassTransformer());

        if (!isBootstrap()) {
            List<ReflectionSupplier> reflectionSuppliers = new ArrayList<>();
            for (ReflectionSupplier supplier : ServiceLoader.load(ReflectionSupplier.class, host.getClassLoader())) {
                reflectionSuppliers.add(supplier);
            }
            ReflectionDependencyListener reflection = new ReflectionDependencyListener(reflectionSuppliers);
            host.registerService(ReflectionDependencyListener.class, reflection);
            host.add(reflection);

            host.add(new PlatformMarkerSupport(host.getPlatformTags()));

            TeaVMCHost cHost = host.getExtension(TeaVMCHost.class);
            if (cHost != null) {
                cHost.addIntrinsic(context -> new DateTimeZoneProviderIntrinsic(context.getProperties()));
            }

            TeaVMWasmHost wasmHost = host.getExtension(TeaVMWasmHost.class);
            if (wasmHost != null) {
                wasmHost.add(context -> new DateTimeZoneProviderIntrinsic(context.getProperties()));
            }
        }

        TeaVMPluginUtil.handleNatives(host, Class.class);
        TeaVMPluginUtil.handleNatives(host, ClassLoader.class);
        TeaVMPluginUtil.handleNatives(host, System.class);
        TeaVMPluginUtil.handleNatives(host, Array.class);
        TeaVMPluginUtil.handleNatives(host, Math.class);

        installMetadata(host.getService(MetadataRegistration.class));
        host.add(new DeclaringClassDependencyListener());
        applyTimeZoneDetection(host);
    }

    private void applyTimeZoneDetection(TeaVMHost host) {
        boolean autodetect = Boolean.parseBoolean(
                host.getProperties().getProperty("java.util.TimeZone.autodetect", "false"));
        if (!autodetect) {
            host.add(new DateTimeZoneProviderPatch());
        }
    }

    private void installMetadata(MetadataRegistration reg) {
        reg.register(new MethodReference(DateTimeZoneProvider.class, "getResource", ResourceMap.class),
                new TimeZoneGenerator());
        reg.register(new MethodReference(DateTimeZoneProvider.class, "getResource", ResourceMap.class),
                new TimeZoneGenerator());

        reg.register(new MethodReference(CurrencyHelper.class, "getCurrencies", ResourceArray.class),
                new CurrenciesGenerator());
        reg.register(new MethodReference(CurrencyHelper.class, "getCountryToCurrencyMap", ResourceMap.class),
                new CountriesGenerator());

        reg.register(new MethodReference(CLDRHelper.class, "getLikelySubtagsMap", ResourceMap.class),
                new LikelySubtagsMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getErasMap", ResourceMap.class),
                new DateSymbolsMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getAmPmMap", ResourceMap.class),
                new DateSymbolsMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getMonthMap", ResourceMap.class),
                new DateSymbolsMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getShortMonthMap", ResourceMap.class),
                new DateSymbolsMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getWeekdayMap", ResourceMap.class),
                new DateSymbolsMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getShortWeekdayMap", ResourceMap.class),
                new DateSymbolsMetadataGenerator());

        reg.register(new MethodReference(CLDRHelper.class, "getTimeZoneLocalizationMap", ResourceMap.class),
                new TimeZoneLocalizationGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getLanguagesMap", ResourceMap.class),
                new LanguageMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getCountriesMap", ResourceMap.class),
                new CountryMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getDefaultLocale", StringResource.class),
                new DefaultLocaleMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getAvailableLocales", ResourceArray.class),
                new AvailableLocalesMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getMinimalDaysInFirstWeek", ResourceMap.class),
                new MinimalDaysInFirstWeekMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getFirstDayOfWeek", ResourceMap.class),
                new FirstDayOfWeekMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getDateFormatMap", ResourceMap.class),
                new DateFormatMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getTimeFormatMap", ResourceMap.class),
                new DateFormatMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getDateTimeFormatMap", ResourceMap.class),
                new DateFormatMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getNumberFormatMap", ResourceMap.class),
                new NumberFormatMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getPercentFormatMap", ResourceMap.class),
                new NumberFormatMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getCurrencyFormatMap", ResourceMap.class),
                new NumberFormatMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getDecimalDataMap", ResourceMap.class),
                new DecimalMetadataGenerator());
        reg.register(new MethodReference(CLDRHelper.class, "getCurrencyMap", ResourceMap.class),
                new CurrencyLocalizationMetadataGenerator());

        reg.register(new MethodReference(Character.class, "obtainDigitMapping", StringResource.class),
                new CharacterMetadataGenerator());
        reg.register(new MethodReference(Character.class, "obtainClasses", StringResource.class),
                new CharacterMetadataGenerator());
        reg.register(new MethodReference(Character.class, "acquireTitleCaseMapping", StringResource.class),
                new CharacterMetadataGenerator());
    }

    @PlatformMarker
    private static boolean isBootstrap() {
        return false;
    }
}
