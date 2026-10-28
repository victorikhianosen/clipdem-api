package com.victor.MovieDownloader.service;

import com.victor.MovieDownloader.exception.InvalidUrlException;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

@Service
public class UrlValidationService {

    public void validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new InvalidUrlException("url must not be blank");
        }

        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            throw new InvalidUrlException("url is not a valid URI");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new InvalidUrlException("Only http and https URLs are supported");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new InvalidUrlException("url must contain a valid host");
        }

        resolveAndCheckAllAddresses(host);
    }

    private void resolveAndCheckAllAddresses(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new InvalidUrlException("Unable to resolve host");
        }

        if (addresses.length == 0) {
            throw new InvalidUrlException("Unable to resolve host");
        }

        for (InetAddress address : addresses) {
            if (isBlockedAddress(address)) {
                throw new InvalidUrlException("url resolves to a disallowed network address");
            }
        }
    }

    private boolean isBlockedAddress(InetAddress address) {
        if (address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        if (address instanceof Inet4Address) {
            byte[] bytes = address.getAddress();
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;

            // 10.0.0.0/8
            if (first == 10) return true;
            // 172.16.0.0/12
            if (first == 172 && second >= 16 && second <= 31) return true;
            // 192.168.0.0/16
            if (first == 192 && second == 168) return true;
            // 169.254.0.0/16 (link-local, also covered above but double-checked)
            if (first == 169 && second == 254) return true;
            // 100.64.0.0/10 (carrier-grade NAT)
            if (first == 100 && second >= 64 && second <= 127) return true;
            // 0.0.0.0/8
            if (first == 0) return true;
        }

        if (address instanceof Inet6Address) {
            byte[] bytes = address.getAddress();
            // fc00::/7 unique local addresses
            if ((bytes[0] & 0xFE) == 0xFC) return true;
        }

        return false;
    }
}
